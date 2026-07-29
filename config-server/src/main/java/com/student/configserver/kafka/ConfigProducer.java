package com.student.configserver.kafka;

import com.student.configserver.model.ConfigMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a config change to the right topic. We use a tiny hand-rolled
 * JSON string instead of pulling in a full ObjectMapper dependency just for
 * this - keeps the message format obvious when you're staring at it in
 * kafka-console-consumer while debugging.
 */
@Component
public class ConfigProducer 
{

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ConfigProducer(KafkaTemplate<String, String> kafkaTemplate) 
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ConfigMessage message) 
    {
        String topic = "config." + message.getService();
        String json = toJson(message);
        // key = config key, so Kafka keeps all updates for the same key on the same partition/order
        kafkaTemplate.send(topic, message.getKey(), json);
    }

    private String toJson(ConfigMessage m) 
    {
        return "{"
                + "\"service\":\"" + escape(m.getService()) + "\","
                + "\"key\":\"" + escape(m.getKey()) + "\","
                + "\"encryptedValue\":\"" + escape(m.getEncryptedValue()) + "\","
                + "\"timestamp\":" + m.getTimestamp()
                + "}";
    }

    private String escape(String s) 
    {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
