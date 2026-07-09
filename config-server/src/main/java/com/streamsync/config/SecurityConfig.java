package com.streamsync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 *
 * In this demo setup, we use HTTP Basic Auth (admin:streamsync123).
 * In production, replace with:
 *   - JWT Bearer tokens (stateless, suitable for microservice-to-microservice calls)
 *   - OAuth2 / OIDC (if you have an identity provider like Keycloak)
 *   - mTLS (mutual TLS) for machine-to-machine auth
 *
 * The /api/health endpoint is intentionally unauthenticated —
 * Apache HTTP Server's mod_proxy_balancer and JMeter need to hit it
 * without credentials for health checks.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disabled for REST API; enable CSRF for browser-facing forms
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/").permitAll()          // Dashboard static files
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});  // Basic Auth for demo — replace with JWT in production

        return http.build();
    }
}
