package com.student.auth.circuitbreaker;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Pretends to call a flaky downstream identity provider (think: an external
 * SSO/OAuth server) to validate a token. Fails about 40% of the time to make
 * the circuit breaker actually trip during a demo instead of just sitting
 * there closed the whole time.
 */
@Component
public class DownstreamAuthProcessor 
{

    private final Random random = new Random();

    @CircuitBreaker(name = "downstreamProcessor", fallbackMethod = "fallback")
    public String verify(String token) 
    {
        if (random.nextInt(100) < 40) 
        {
            throw new RuntimeException("Downstream identity provider timed out");
        }
        return "Token '" + token + "' verified successfully via downstream identity provider";
    }

    private String fallback(String token, Throwable t) 
    {
        return "Identity provider unavailable (circuit breaker fallback) - falling back to cached session for '" + token + "'. Reason: " + t.getMessage();
    }
}
