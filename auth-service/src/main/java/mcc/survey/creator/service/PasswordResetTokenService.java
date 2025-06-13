package mcc.survey.creator.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    private static final long EXPIRATION_MINUTES = 60; // 1 hour

    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    public boolean isTokenExpired(LocalDateTime tokenExpiryDate) {
        if (tokenExpiryDate == null) {
            return true; // Or throw an IllegalArgumentException, depending on desired handling
        }
        return tokenExpiryDate.isBefore(LocalDateTime.now());
    }

    public LocalDateTime getExpiryDate() {
        return LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
    }
}
