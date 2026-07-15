package com.streamsync.controller;

import com.streamsync.model.ConfigEntry;
import com.streamsync.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * REST API for the StreamSync Config Server.
 *
 * All endpoints are secured via Spring Security (Basic Auth in dev, JWT in prod).
 * The Apache HTTP Server sits in front and terminates TLS before traffic reaches here.
 *
 * Endpoint map:
 *   PUT    /api/config/{service}/{key}       — push a config update
 *   DELETE /api/config/{service}/{key}       — delete a config key
 *   GET    /api/config/{service}             — get all config for a service
 *   GET    /api/config/{service}/{key}       — get a single config value
 *   GET    /api/services                     — list all registered services
 *   GET    /api/health                       — health check (unauthenticated)
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")  // Dashboard on same Apache server — tighten in production
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Push a config update.
     * The request body should contain: value, reason.
     * service and key come from the path.
     * changedBy is extracted from the authenticated principal.
     */
    @PutMapping("/config/{service}/{key}")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String service,
            @PathVariable String key,
            @RequestBody Map<String, String> body,
            Principal principal) {

        String value  = body.get("value");
        String reason = body.getOrDefault("reason", "");

        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "value is required"));
        }

        ConfigEntry entry = ConfigEntry.builder()
                .service(service)
                .key(key)
                .value(value)
                .reason(reason)
                .changedBy(principal != null ? principal.getName() : "anonymous")
                .build();

        configService.updateConfig(entry);

        return ResponseEntity.ok(Map.of(
                "status",  "published",
                "service", service,
                "key",     key,
                "message", "Config update encrypted and published to Kafka"
        ));
    }

    /**
     * Delete a config key — broadcasts DELETE event to all consumers.
     */
    @DeleteMapping("/config/{service}/{key}")
    public ResponseEntity<Map<String, Object>> deleteConfig(
            @PathVariable String service,
            @PathVariable String key,
            Principal principal) {

        configService.deleteConfig(service, key,
                principal != null ? principal.getName() : "anonymous");

        return ResponseEntity.ok(Map.of(
                "status",  "deleted",
                "service", service,
                "key",     key
        ));
    }

    /**
     * Get all current config for a service.
     * Reads from Redis — this is the live source of truth for the dashboard.
     */
    @GetMapping("/config/{service}")
    public ResponseEntity<Map<Object, Object>> getServiceConfig(@PathVariable String service) {
        Map<Object, Object> config = configService.getCurrentConfig(service);
        return ResponseEntity.ok(config);
    }

    /**
     * Get a single config value.
     */
    @GetMapping("/config/{service}/{key}")
    public ResponseEntity<Map<String, Object>> getConfigValue(
            @PathVariable String service,
            @PathVariable String key) {

        String value = configService.getConfigValue(service, key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "service", service,
                "key",     key,
                "value",   value
        ));
    }

    /**
     * List all services that have config registered.
     */
    @GetMapping("/services")
    public ResponseEntity<Set<String>> listServices() {
        return ResponseEntity.ok(configService.listServices());
    }

    /**
     * Health check — used by Apache HTTP Server mod_status and JMeter baseline tests.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",  "UP",
                "service", "streamsync-config-server"
        ));
    }
}
