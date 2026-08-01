package com.student.inventory.controller;

import com.student.inventory.circuitbreaker.DownstreamInventoryProcessor;
import com.student.inventory.store.ConfigStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController 
{

    private final ConfigStore configStore;
    private final DownstreamInventoryProcessor processor;

    public InventoryController(ConfigStore configStore, DownstreamInventoryProcessor processor) 
    {
        this.configStore = configStore;
        this.processor = processor;
    }

    /** What the dashboard polls every 3s to show this service's live config state. */
    @GetMapping("/config")
    public Map<String, Object> currentConfig() 
    {
        return Map.of(
                "service", "inventory",
                "values", configStore.getAll(),
                "count", configStore.size(),
                "lastUpdated", configStore.getLastUpdated()
        );
    }

    /** Circuit breaker demo: call this repeatedly and watch it flip between success / fallback / open-circuit. */
    @PostMapping("/check-stock")
    public Map<String, String> checkStock(@RequestParam(defaultValue = "SKU-1001") String sku) 
    {
        String result = processor.checkStock(sku);
        return Map.of("result", result);
    }

    @GetMapping("/health")
    public Map<String, String> health() 
    {
        return Map.of("status", "UP", "service", "inventory");
    }
}
