package com.spms.backend.service;

import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Şifre hash'leme ve doğrulama işlemlerini tek merkezde yöneten servis.
 *
 * Eskiden bu metotlar ProfessorRegistrationService içinde private'tı.
 * Artık hem ProfessorRegistrationService hem de PasswordService
 * bu ortak servisi kullanıyor.
 *
 * Algoritma: PBKDF2WithHmacSHA256 (65,536 iterasyon, 16 byte salt)
 * Çıktı formatı: "pbkdf2$65536$<salt_base64>$<hash_base64>"
 */
@Service
public class PasswordHashingService {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_ITERATIONS = 65_536;
    private static final int HASH_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Düz metin şifreyi hash'ler.
     *
     * Örnek girdi:  "TempPass123"
     * Örnek çıktı:  "pbkdf2$65536$dG9wIHNlY3JldA==$aGFzaGVk..."
     */
    public String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = hash(password, salt, HASH_ITERATIONS);
        return "pbkdf2$" + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Düz metin şifrenin, veritabanındaki hash ile eşleşip eşleşmediğini kontrol eder.
     *
     * Örnek: matchesPassword("TempPass123", "pbkdf2$65536$abc...$xyz...")
     *        → true (eşleşiyorsa) veya false
     */
    public boolean matchesPassword(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return false;
        }

        String[] parts = passwordHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
        byte[] candidateHash = hash(rawPassword, salt, iterations);
        return MessageDigest.isEqual(expectedHash, candidateHash);
    }

    /**
     * PBKDF2 hash hesaplama (iç metot).
     */
    private byte[] hash(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_KEY_LENGTH);
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            return secretKeyFactory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash password.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}