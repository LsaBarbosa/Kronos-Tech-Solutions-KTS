package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {
    @Value("${frontend.base-url-record}")
    private String recordUrl;
    @Value("${frontend.base-url-plataform}")
    private String plataformUrl;
    @Value("${frontend.base-url-local}")
    private String local;
    @Value("${frontend.base-url-local-2}")
    private String local_2;

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;
    private final RateLimitProperties rateLimitProperties;
    private final AuthCookieService authCookieService;

    public SecurityConfig(JwtUtils jwtUtils, CustomUserDetailsService uds,
                          DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint,
                          RateLimitProperties rateLimitProperties,
                          AuthCookieService authCookieService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = uds;
        this.delegatedAuthenticationEntryPoint = delegatedAuthenticationEntryPoint;
        this.rateLimitProperties = rateLimitProperties;
        this.authCookieService = authCookieService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var termsFilter = new TermsValidationFilter(jwtUtils, authCookieService);
        var jwtFilter = new JwtAuthenticationFilter(jwtUtils, userDetailsService, authCookieService);
        var rateLimitFilter = new RateLimitFilter(rateLimitProperties);
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers
                                ("/auth/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/auth/recover-password",
                                        "/",
                                        "/healthz",
                                        "/actuator/health/**",
                                        "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(customizer -> customizer.authenticationEntryPoint(delegatedAuthenticationEntryPoint))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(termsFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(buildAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cache-Control",
                "Pragma"
        ));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> buildAllowedOrigins() {
        List<String> origins = Stream.of(recordUrl, plataformUrl, local, local_2)
                .map(this::normalizeOrigin)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException("At least one frontend origin must be configured for CORS.");
        }

        List<String> validatedOrigins = new ArrayList<>(origins.size());
        for (String origin : origins) {
            if ("*".equals(origin)) {
                throw new IllegalStateException("Wildcard '*' is not allowed when allowCredentials(true) is enabled.");
            }

            URI uri = URI.create(origin);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalStateException("Invalid frontend origin configured: " + origin);
            }
            if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
                throw new IllegalStateException("Frontend origin must not contain path segments: " + origin);
            }
            if (uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalStateException("Frontend origin must not contain query or fragment: " + origin);
            }
            validatedOrigins.add(origin);
        }

        return validatedOrigins;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private String normalizeOrigin(String origin) {
        if (origin == null) {
            return "";
        }

        var normalized = origin.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
