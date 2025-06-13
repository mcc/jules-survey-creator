package mcc.survey.creator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenServiceTest {

    private PasswordResetTokenService passwordResetTokenService;

    @BeforeEach
    void setUp() {
        passwordResetTokenService = new PasswordResetTokenService();
    }

    @Test
    void generateToken_shouldReturnNonNullNonEmptyString() {
        String token = passwordResetTokenService.generateToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getExpiryDate_shouldReturnFutureDate() {
        LocalDateTime expiryDate = passwordResetTokenService.getExpiryDate();
        assertNotNull(expiryDate);
        assertTrue(expiryDate.isAfter(LocalDateTime.now()));
    }

    @Test
    void isTokenExpired_withPastDate_shouldReturnTrue() {
        LocalDateTime pastDate = LocalDateTime.now().minusHours(1);
        assertTrue(passwordResetTokenService.isTokenExpired(pastDate));
    }

    @Test
    void isTokenExpired_withFutureDate_shouldReturnFalse() {
        LocalDateTime futureDate = LocalDateTime.now().plusHours(1);
        assertFalse(passwordResetTokenService.isTokenExpired(futureDate));
    }

    @Test
    void isTokenExpired_withNullDate_shouldReturnTrue() {
        assertTrue(passwordResetTokenService.isTokenExpired(null));
    }

    @Test
    void isTokenExpired_withVeryFutureDate_shouldReturnFalse() {
        LocalDateTime veryFutureDate = LocalDateTime.now().plusDays(30);
        assertFalse(passwordResetTokenService.isTokenExpired(veryFutureDate));
    }

    @Test
    void isTokenExpired_withVeryPastDate_shouldReturnTrue() {
        LocalDateTime veryPastDate = LocalDateTime.now().minusDays(30);
        assertTrue(passwordResetTokenService.isTokenExpired(veryPastDate));
    }
}
