package com.student.auth.redis;

import com.student.auth.store.ConfigStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * On startup, before any Kafka messages have necessarily arrived, warm up
 * the local ConfigStore straight from Redis. This means the service is
 * immediately useful right after boot instead of sitting there empty until
 * Kafka replays history (or waiting on the next push).
 *
 * Note: Redis holds plaintext (see config-server's RedisConfigService) - by
 * the time a value is in Redis it's already trusted internal infrastructure
 * state, so no decryption needed here. Only the Kafka transport is encrypted.
 */
@Component
public class RedisBootstrap 
{

    private final StringRedisTemplate redisTemplate;
    private final ConfigStore configStore;
    private final String serviceName;

    public RedisBootstrap(StringRedisTemplate redisTemplate,
                           ConfigStore configStore,
                           @Value("${app.service-name}") String serviceName) {
        this.redisTemplate = redisTemplate;
        this.configStore = configStore;
        this.serviceName = serviceName;
    }

    @PostConstruct
    public void bootstrapFromRedis() 
    {
        String pattern = "config:" + serviceName + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) 
        {
            return;
        }
        String prefix = "config:" + serviceName + ":";
        for (String fullKey : keys) 
        {
            String shortKey = fullKey.substring(prefix.length());
            String value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) 
            {
                configStore.put(shortKey, value);
            }
        }
    }
}
