package com.student.auth.store;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The service's local, in-memory view of its own config. A ConcurrentHashMap
 * is enough here because we have exactly one Kafka consumer thread writing
 * and any number of HTTP threads reading - no need for anything fancier.
 */
@Component
public class ConfigStore 
{

    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
    private volatile long lastUpdated = 0L;

    public void put(String key, String value) 
    {
        values.put(key, value);
        lastUpdated = System.currentTimeMillis();
    }

    public String get(String key) 
    {
        return values.get(key);
    }

    public Map<String, String> getAll() 
    {
        return Map.copyOf(values);
    }

    public long getLastUpdated() 
    {
        return lastUpdated;
    }

    public int size() 
    {
        return values.size();
    }
}
