package com.student.auth.controller;

import com.student.auth.circuitbreaker.DownstreamAuthProcessor;
import com.student.auth.store.ConfigStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final ConfigStore configStore;
    private final DownstreamAuthProcessor processor;

    public AuthController(ConfigStore configStore, DownstreamAuthProcessor processor) {
        this.configStore = configStore;
        this.processor = processor;
    }

    /** What the dashboard polls every 3s to show this service's live config state. */
    @GetMapping("/config")
    public Map<String, Object> currentConfig() {
        return Map.of(
                "service", "auth",
                "values", configStore.getAll(),
                "count", configStore.size(),
                "lastUpdated", configStore.getLastUpdated()
        );
    }

    /** Circuit breaker demo: call this repeatedly and watch it flip between success / fallback / open-circuit. */
    @PostMapping("/verify-token")
    public Map<String, String> verifyToken(@RequestParam(defaultValue = "demo-token") String token) {
        String result = processor.verify(token);
        return Map.of("result", result);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "auth");
    }
}
