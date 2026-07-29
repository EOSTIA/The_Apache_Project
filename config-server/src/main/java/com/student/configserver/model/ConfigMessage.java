package com.student.configserver.model;

/**
 * The payload we publish to Kafka. The value is ALWAYS the AES-GCM encrypted
 * ciphertext (base64) - never the plaintext. Consumers decrypt it on their side
 * using the same shared secret key.
 */
public class ConfigMessage 
{

    private String service;      // e.g. "payments"
    private String key;          // e.g. "db.connection.timeout"
    private String encryptedValue;
    private long timestamp;

    public ConfigMessage() {
    }

    public ConfigMessage(String service, String key, String encryptedValue, long timestamp) 
    {
        this.service = service;
        this.key = key;
        this.encryptedValue = encryptedValue;
        this.timestamp = timestamp;
    }

    public String getService() 
    {
        return service;
    }

    public void setService(String service) 
    {
        this.service = service;
    }

    public String getKey() 
    {
        return key;
    }

    public void setKey(String key) 
    {
        this.key = key;
    }

    public String getEncryptedValue() 
    {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) 
    {
        this.encryptedValue = encryptedValue;
    }

    public long getTimestamp() 
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp) 
    {
        this.timestamp = timestamp;
    }
}
