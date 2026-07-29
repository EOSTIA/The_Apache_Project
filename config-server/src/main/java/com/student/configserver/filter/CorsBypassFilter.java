package com.student.configserver.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * Deliberately bypasses Spring Security entirely.
 *
 * Background: Spring Security's built-in CORS integration (both the manual
 * CorsConfigurationSource bean approach, and the "auto-detect from
 * WebMvcConfigurer" approach) both produced "Invalid CORS request" 403s in
 * this project's setup, for reasons that weren't worth chasing further -
 * some interaction between Security's filter ordering and how it resolves
 * the CORS config bean. Rather than keep debugging Spring's internals, this
 * filter is registered with HIGHEST_PRECEDENCE so it runs as the very first
 * thing in the whole filter chain - BEFORE Spring Security's
 * DelegatingFilterProxy even sees the request. It adds the CORS headers
 * itself and, for OPTIONS preflight requests specifically, answers 200
 * immediately without forwarding the request any further. Since Security
 * never sees the preflight at all, none of its CORS logic can reject it.
 *
 * For the ACTUAL POST/GET requests (not the preflight), this filter just
 * adds the header and lets the request continue into Spring Security
 * normally - authentication on /api/config/push still applies exactly as
 * before.
 */
@Configuration
public class CorsBypassFilter implements Filter 
{

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        if (origin != null) 
        {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } 
        else 
        {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) 
        {
            response.setStatus(HttpServletResponse.SC_OK);
            return; // short-circuit - never reaches Spring Security
        }

        chain.doFilter(req, res);
    }

    @Bean
    public FilterRegistrationBean<CorsBypassFilter> corsBypassFilterRegistration() 
    {
        FilterRegistrationBean<CorsBypassFilter> registration = new FilterRegistrationBean<>(new CorsBypassFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // run before Spring Security's filter chain
        return registration;
    }
}