package com.student.configserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * HTTP Basic auth in front of the config-push endpoint only. Everything else (GET reads, health) stays open.
 *
 * CORS is intentionally NOT configured here (no .cors(...) call). It's
 * handled entirely by CorsBypassFilter.java, which runs before Spring
 * Security's filter chain even starts. Adding .cors(...) here too would risk Spring Security
 * trying to add its own Access-Control-* headers on top
 * of the ones CorsBypassFilter already set,
 * which browsers reject as an invalid duplicate header.
 */

@Configuration
public class SecurityConfig 
{

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception 
    {
        http
            .csrf(csrf -> csrf.disable()) // disabled for demo simplicity - for real world application can use CSRF tokens or be pure API + token auth
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // belt-and-braces; CorsBypassFilter already short-circuits OPTIONS before this is even reached
                .requestMatchers("/api/config/push").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() 
    {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}