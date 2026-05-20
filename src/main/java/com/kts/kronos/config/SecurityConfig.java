package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.in.web.exceptions.JsonAccessDeniedHandler;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtAuthenticationFilter;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.out.security.TermsValidationFilter;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${frontend.base-url-record}")
    private String recordUrl;
    @Value("${frontend.base-url-plataform}")
    private String plataformUrl;
    @Value("${frontend.base-url-local}")
    private String local;
    @Value("${frontend.base-url-local-2}")
    private String local_2;
    @Value("${frontend.allowed-origins:}")
    private String allowedOriginsRaw;
    @Value("${app.security.public-docs-enabled:false}")
    private boolean publicDocsEnabled;

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;
    private final TokenBlacklistProvider tokenBlacklistProvider;
    private final AuthCookieService authCookieService;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public SecurityConfig(
            JwtUtils jwtUtils,
            CustomUserDetailsService uds,
            DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint,
            TokenBlacklistProvider tokenBlacklistProvider,
            AuthCookieService authCookieService,
            JsonAccessDeniedHandler jsonAccessDeniedHandler,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = uds;
        this.delegatedAuthenticationEntryPoint = delegatedAuthenticationEntryPoint;
        this.tokenBlacklistProvider = tokenBlacklistProvider;
        this.authCookieService = authCookieService;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var termsFilter = new TermsValidationFilter(jwtUtils, authCookieService, handlerExceptionResolver);
        var jwtFilter = new JwtAuthenticationFilter(jwtUtils, userDetailsService, tokenBlacklistProvider, authCookieService);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .ignoringRequestMatchers(
                                post("/auth/login"),
                                post("/auth/login-face"),
                                post("/auth/recover-password"),
                                post("/auth/reset-password"),
                                post("/auth/logout"),
                                post("/geolocation/resolve")
                        )
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(customizer -> customizer
                        .authenticationEntryPoint(delegatedAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(termsFilter, JwtAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers(
                    org.springframework.http.HttpMethod.POST,
                    "/auth/login",
                    "/auth/login-face",
                    "/auth/recover-password",
                    "/auth/reset-password",
                    "/auth/logout"
            ).permitAll();
            auth.requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/auth/csrf"
            ).permitAll();
            auth.requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/observability/status"
            ).permitAll();

            auth.requestMatchers(
                    EndpointRequest.to("health", "info", "metrics", "prometheus")
            ).permitAll();

            if (publicDocsEnabled) {
                auth.requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                ).permitAll();
            }

            auth.anyRequest().authenticated();
        });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(resolveAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        if (allowedOriginsRaw != null && !allowedOriginsRaw.isBlank()) {
            return Arrays.stream(allowedOriginsRaw.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .distinct()
                    .toList();
        }

        return Arrays.asList(recordUrl, plataformUrl, local, local_2).stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("KRONOS_CSRF_TOKEN");
        repository.setCookiePath("/");
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie
                .secure(true)
                .sameSite("None")
                .path("/")
        );
        return repository;
    }

    private RequestMatcher post(String path) {
        return request -> {
            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                return false;
            }
            return path.equals(request.getServletPath()) || path.equals(request.getRequestURI());
        };
    }
}
