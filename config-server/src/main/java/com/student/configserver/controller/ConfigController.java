package com.student.configserver.controller;

import com.student.configserver.crypto.AesGcmEncryptor;
import com.student.configserver.kafka.ConfigProducer;
import com.student.configserver.model.ConfigMessage;
import com.student.configserver.redis.RedisConfigService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/config")
// CORS is bypassd - do not add @CrossOrigin here, causes conflicting CORS behavior
public class ConfigController 
{

    private static final Set<String> VALID_SERVICES = Set.of("payments", "auth", "inventory");

    private final AesGcmEncryptor encryptor;
    private final ConfigProducer producer;
    private final RedisConfigService redisConfigService;

    public ConfigController(AesGcmEncryptor encryptor, ConfigProducer producer, RedisConfigService redisConfigService) 
    {
        this.encryptor = encryptor;
        this.producer = producer;
        this.redisConfigService = redisConfigService;
    }

    public record PushRequest(@NotBlank String service, @NotBlank String key, @NotBlank String value) {
    }

    /**
     * Pushes one config value out to a service:
     *  1. Save the plaintext straight into Redis (source of truth, used for warm-start bootstrap).
     *  2. Encrypt it with AES-GCM.
     *  3. Publish the encrypted value on that service's Kafka topic.
     *
     * Requires HTTP Basic auth (see SecurityConfig) - this is the one
     * "write" endpoint in the whole system, everything else is read-only.
     */
    @PostMapping("/push")
    public Map<String, Object> push(@RequestBody PushRequest request) 
    {
        if (!VALID_SERVICES.contains(request.service())) 
        {
            throw new IllegalArgumentException("Unknown service: " + request.service() + ". Must be one of " + VALID_SERVICES);
        }

        redisConfigService.save(request.service(), request.key(), request.value());

        String encrypted = encryptor.encrypt(request.value());
        long now = Instant.now().toEpochMilli();

        ConfigMessage message = new ConfigMessage(request.service(), request.key(), encrypted, now);
        producer.publish(message);

        return Map.of(
                "status", "published",
                "service", request.service(),
                "key", request.key(),
                "topic", "config." + request.service(),
                "timestamp", now
        );
    }

    /** Lets the dashboard show what's currently sitting in Redis for a given service, without going through Kafka. */
    @GetMapping("/redis/{service}")
    public Map<String, String> getRedisState(@PathVariable String service) 
    {
        return redisConfigService.getAllForService(service);
    }

    @GetMapping("/services")
    public List<String> listServices() 
    {
        return List.copyOf(VALID_SERVICES);
    }

    @GetMapping("/health")
    public Map<String, String> health() 
    {
        return Map.of("status", "UP", "service", "config-server");
    }
}