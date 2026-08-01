package com.student.payments.circuitbreaker;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Pretends to be a flaky third-party payment gateway: it fails about 40% of
 * the time. Wrapped with @CircuitBreaker so that once failures pile up past
 * the configured threshold (see application.yml), the breaker "opens" and
 * short-circuits straight to the fallback instead of calling this method at
 * all - which is exactly what you'd want in production to stop hammering a
 * struggling downstream dependency.
 */
@Component
public class DownstreamPaymentProcessor 
{

    private final Random random = new Random();

    @CircuitBreaker(name = "downstreamProcessor", fallbackMethod = "fallback")
    public String charge(String amount) 
    {
        if (random.nextInt(100) < 40) 
        {
            throw new RuntimeException("Downstream payment gateway timed out");
        }
        return "Charged $" + amount + " successfully via downstream gateway";
    }

    // Fallback signature must match: same params + a Throwable at the end.
    private String fallback(String amount, Throwable t) 
    {
        return "Downstream gateway unavailable (circuit breaker fallback) - queued $" + amount + " for retry. Reason: " + t.getMessage();
    }
}
