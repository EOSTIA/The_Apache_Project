package com.student.configserver.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis holds the PLAINTEXT source-of-truth for every config key, keyed as
 * "config:<service>:<key>". This is deliberate: Redis is our fast, durable
 * "current state" store that any service can use to warm up its local cache
 * on startup - before the first Kafka message even arrives. Kafka is purely
 * the "here's a live update, and it's encrypted in transit" channel.
 */
@Service
public class RedisConfigService 
{

    private final StringRedisTemplate redisTemplate;

    public RedisConfigService(StringRedisTemplate redisTemplate) 
    {
        this.redisTemplate = redisTemplate;
    }

    public void save(String service, String key, String plainValue) 
    {
        redisTemplate.opsForValue().set(redisKey(service, key), plainValue);
    }

    public Map<String, String> getAllForService(String service) 
    {
        Map<String, String> result = new HashMap<>();
        String pattern = "config:" + service + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys == null) 
            return result;
        for (String fullKey : keys) 
        {
            String shortKey = fullKey.substring(("config:" + service + ":").length());
            result.put(shortKey, redisTemplate.opsForValue().get(fullKey));
        }
        return result;
    }

    private String redisKey(String service, String key) 
    {
        return "config:" + service + ":" + key;
    }
}
