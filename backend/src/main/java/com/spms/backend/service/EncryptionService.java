package com.spms.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {
    private static final String ALGORITHM = "AES";

    @Value("${app.encryption.key:MySuperSecretKey1234567890123456}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM));
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)));
        } catch (IllegalArgumentException e) {
            // Base64 decode failed - likely plain text or malformed data
            // Return as-is to handle legacy unencrypted data
            return encryptedText;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
