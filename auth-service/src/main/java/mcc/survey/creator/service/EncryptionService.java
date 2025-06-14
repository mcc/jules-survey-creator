package mcc.survey.creator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // GCM standard IV length is 12 bytes (96 bits)
    private static final int GCM_TAG_LENGTH_BITS = 128; // GCM standard auth tag length

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.encryption.key}") String keyString) {
        if (keyString == null || keyString.length() < 16) { // Basic check for key length
            logger.error("Master encryption key is null or too short. Please ensure it is properly configured.");
            throw new IllegalArgumentException("Master encryption key must be at least 16 characters long.");
        }
        // Use first 16, 24, or 32 bytes of the key string for AES
        int keyLengthBytes;
        if (keyString.length() >= 32) {
            keyLengthBytes = 32; // AES-256
        } else if (keyString.length() >= 24) {
            keyLengthBytes = 24; // AES-192
        } else {
            keyLengthBytes = 16; // AES-128
        }
        byte[] keyBytes = new byte[keyLengthBytes];
        System.arraycopy(keyString.getBytes(StandardCharsets.UTF_8), 0, keyBytes, 0, keyLengthBytes);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");

        // Test encryption/decryption with a dummy value to ensure the key and JCE policies are correct
        try {
            encrypt("Initialization test");
            logger.info("EncryptionService initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize EncryptionService. This might be due to JCE policy restrictions (e.g., for key lengths > 128 bits). Please ensure appropriate JCE Unlimited Strength Jurisdiction Policy Files are installed if using older Java versions or specific JREs.", e);
            // Depending on policy, re-throw or handle appropriately. For now, logging and allowing startup.
            // throw new IllegalStateException("Failed to initialize EncryptionService due to cryptographic restrictions.", e);
        }
    }

    public String encrypt(String data) {
        if (data == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to the encrypted data for use during decryption
            byte[] encryptedOutput = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedOutput, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, encryptedOutput, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(encryptedOutput);
        } catch (Exception e) {
            logger.error("Error during encryption", e);
            throw new EncryptionOperationException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);

            // Extract IV from the beginning of the decoded bytes
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(decodedBytes, 0, iv, 0, iv.length);

            byte[] actualEncryptedData = new byte[decodedBytes.length - iv.length];
            System.arraycopy(decodedBytes, iv.length, actualEncryptedData, 0, actualEncryptedData.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(actualEncryptedData);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error during decryption", e);
            throw new EncryptionOperationException("Error decrypting data", e);
        }
    }

    // Custom exception class for better error handling context
    public static class EncryptionOperationException extends RuntimeException {
        public EncryptionOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
