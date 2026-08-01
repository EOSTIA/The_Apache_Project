package com.student.payments.controller;

import com.student.payments.circuitbreaker.DownstreamPaymentProcessor;
import com.student.payments.store.ConfigStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentsController 
{

    private final ConfigStore configStore;
    private final DownstreamPaymentProcessor processor;

    public PaymentsController(ConfigStore configStore, DownstreamPaymentProcessor processor) 
    {
        this.configStore = configStore;
        this.processor = processor;
    }

    /** What the dashboard polls every 3s to show this service's live config state. */
    @GetMapping("/config")
    public Map<String, Object> currentConfig() 
    {
        return Map.of(
                "service", "payments",
                "values", configStore.getAll(),
                "count", configStore.size(),
                "lastUpdated", configStore.getLastUpdated()
        );
    }

    /** Circuit breaker demo: call this repeatedly and watch it flip between success / fallback / open-circuit. */
    @PostMapping("/process")
    public Map<String, String> process(@RequestParam(defaultValue = "100") String amount) 
    {
        String result = processor.charge(amount);
        return Map.of("result", result);
    }

    @GetMapping("/health")
    public Map<String, String> health() 
    {
        return Map.of("status", "UP", "service", "payments");
    }
}
