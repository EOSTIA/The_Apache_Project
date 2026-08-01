package com.student.payments.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Same AES-256-GCM scheme as config-server (see that module for the format
 * notes). Every microservice ships its own copy of this class on purpose -
 * there's no shared library module in this project, to keep the build dead
 * simple (no need to `mvn install` a common jar before building the others).
 */
@Component
public class AesGcmEncryptor 
{

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;

    public AesGcmEncryptor(@Value("${app.crypto.secret-key-base64}") String secretKeyBase64) 
    {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String decrypt(String encryptedBase64) 
    {
        try 
        {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) 
        {
            throw new RuntimeException("Failed to decrypt config value - wrong key or tampered data?", e);
        }
    }
}
