package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtAuthenticationFilter;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.out.security.TermsValidationFilter;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.Arrays;

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
    @Value("${app.security.public-docs-enabled:false}")
    private boolean publicDocsEnabled;

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;
    private final TokenBlacklistProvider tokenBlacklistProvider;

    public SecurityConfig(JwtUtils jwtUtils, CustomUserDetailsService uds, DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint, TokenBlacklistProvider tokenBlacklistProvider) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = uds;
        this.delegatedAuthenticationEntryPoint = delegatedAuthenticationEntryPoint;
        this.tokenBlacklistProvider = tokenBlacklistProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var termsFilter = new TermsValidationFilter(jwtUtils);
        var jwtFilter = new JwtAuthenticationFilter(jwtUtils, userDetailsService, tokenBlacklistProvider);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(customizer -> customizer.authenticationEntryPoint(delegatedAuthenticationEntryPoint))
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
                    "/actuator/health",
                    "/actuator/health/**"
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
        configuration.setAllowedOrigins(Arrays.asList(recordUrl,plataformUrl,local,local_2));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
