package com.student.auth.kafka;

import com.student.auth.crypto.AesGcmEncryptor;
import com.student.auth.store.ConfigStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens on config.auth, decrypts each incoming value, and writes it
 * into the local ConfigStore. We parse the tiny hand-rolled JSON with a
 * regex instead of pulling in Jackson - the message shape is fixed and
 * simple enough that this stays readable, and it avoids adding a JSON
 * dependency just for 3 fields.
 */
@Component
public class ConfigConsumer 
{

    // matches: "key":"...","encryptedValue":"..."
    private static final Pattern KEY_PATTERN = Pattern.compile("\"key\":\"(.*?)\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"encryptedValue\":\"(.*?)\"");

    private final AesGcmEncryptor encryptor;
    private final ConfigStore configStore;

    public ConfigConsumer(AesGcmEncryptor encryptor, ConfigStore configStore) 
    {
        this.encryptor = encryptor;
        this.configStore = configStore;
    }

    @KafkaListener(topics = "${app.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onConfigMessage(String rawJson) 
    {
        String key = extract(KEY_PATTERN, rawJson);
        String encryptedValue = extract(VALUE_PATTERN, rawJson);

        if (key == null || encryptedValue == null) 
        {
            System.err.println("[auth] Skipping malformed message: " + rawJson);
            return;
        }

        String plainValue = encryptor.decrypt(encryptedValue);
        configStore.put(key, plainValue);

        System.out.println("[auth] Applied config update: " + key + " = " + plainValue);
    }

    private String extract(Pattern pattern, String json) 
    {
        Matcher m = pattern.matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private String unescape(String s) 
    {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
