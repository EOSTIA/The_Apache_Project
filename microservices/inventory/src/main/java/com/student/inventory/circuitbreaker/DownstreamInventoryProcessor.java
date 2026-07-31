package com.student.inventory.circuitbreaker;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Pretends to call a flaky downstream warehouse/stock system to check stock
 * levels for a SKU. Fails about 40% of the time so the circuit breaker has
 * something to actually react to during a demo.
 */
@Component
public class DownstreamInventoryProcessor 
{

    private final Random random = new Random();

    @CircuitBreaker(name = "downstreamProcessor", fallbackMethod = "fallback")
    public String checkStock(String sku) 
    {
        if (random.nextInt(100) < 40) 
        {
            throw new RuntimeException("Downstream warehouse system timed out");
        }
        int qty = random.nextInt(200);
        return "SKU '" + sku + "' has " + qty + " units in stock (live check)";
    }

    private String fallback(String sku, Throwable t) 
    {
        return "Warehouse system unavailable (circuit breaker fallback) - returning last known stock for '" + sku + "'. Reason: " + t.getMessage();
    }
}
