package mcc.survey.creator.service;

import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.util.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    // Tests for initiatePasswordReset
    @Test
    void initiatePasswordReset_userFound_shouldSetTokenAndSendEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordResetTokenService.generateToken()).thenReturn("testToken");
        when(passwordResetTokenService.getExpiryDate()).thenReturn(LocalDateTime.now().plusHours(1));

        userService.initiatePasswordReset("test@example.com");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordResetTokenService).generateToken();
        verify(passwordResetTokenService).getExpiryDate();
        assertNotNull(testUser.getResetPasswordToken());
        assertEquals("testToken", testUser.getResetPasswordToken());
        assertNotNull(testUser.getResetPasswordTokenExpiry());
        verify(userRepository).save(testUser);
        verify(emailService).sendPasswordResetEmail("test@example.com", "testToken");
    }

    @Test
    void initiatePasswordReset_userNotFound_shouldLogWarningAndNotProceed() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Current implementation logs and returns void, does not throw ResourceNotFoundException directly from this method.
        // If it were to throw, the test would be:
        // assertThrows(ResourceNotFoundException.class, () -> userService.initiatePasswordReset("unknown@example.com"));

        userService.initiatePasswordReset("unknown@example.com");

        verify(userRepository).findByEmail("unknown@example.com");
        verify(passwordResetTokenService, never()).generateToken();
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // Tests for completePasswordReset
    @Test
    void completePasswordReset_validToken_shouldResetPassword() {
        testUser.setResetPasswordToken("validToken");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetPasswordToken("validToken")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenService.isTokenExpired(any(LocalDateTime.class))).thenReturn(false);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        boolean result = userService.completePasswordReset("validToken", "newPassword");

        assertTrue(result);
        assertEquals("newEncodedPassword", testUser.getPassword());
        assertNull(testUser.getResetPasswordToken());
        assertNull(testUser.getResetPasswordTokenExpiry());
        verify(userRepository).save(testUser);
    }

    @Test
    void completePasswordReset_userNotFoundByToken_shouldReturnFalse() {
        when(userRepository.findByResetPasswordToken("invalidToken")).thenReturn(Optional.empty());

        boolean result = userService.completePasswordReset("invalidToken", "newPassword");

        assertFalse(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completePasswordReset_expiredToken_shouldReturnFalse() {
        testUser.setResetPasswordToken("expiredToken");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByResetPasswordToken("expiredToken")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenService.isTokenExpired(any(LocalDateTime.class))).thenReturn(true);

        boolean result = userService.completePasswordReset("expiredToken", "newPassword");

        assertFalse(result);
        assertEquals("encodedPassword", testUser.getPassword()); // Password not changed
        assertNotNull(testUser.getResetPasswordToken()); // Token not cleared by this method directly in this path
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completePasswordReset_tokenValidButUserHasNoExpiryDate_shouldReturnFalseAndNotUpdate() {
        // This scenario implies inconsistent data, possibly a token was set without an expiry.
        testUser.setResetPasswordToken("validTokenNoExpiry");
        testUser.setResetPasswordTokenExpiry(null); // Explicitly null

        when(userRepository.findByResetPasswordToken("validTokenNoExpiry")).thenReturn(Optional.of(testUser));
        // isTokenExpired(null) in PasswordResetTokenService returns true (expired)
        when(passwordResetTokenService.isTokenExpired(null)).thenReturn(true);


        boolean result = userService.completePasswordReset("validTokenNoExpiry", "newPassword");

        assertFalse(result);
        assertEquals("encodedPassword", testUser.getPassword()); // Password should not change
        verify(userRepository, never()).save(any(User.class)); // No save operation
    }

    @Test
    void changePassword_Success_ValidNewPassword() { // Renamed for clarity
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // Ensure the mock for passwordEncoder.matches uses the actual user's current encoded password
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("ValidNewP@ss1")).thenReturn("encodedValidNewPassword");

        // This call should not throw an exception
        assertDoesNotThrow(() -> {
            userService.changePassword("testuser", "oldPassword", "ValidNewP@ss1");
        });

        verify(passwordEncoder).encode("ValidNewP@ss1");
        assertEquals("encodedValidNewPassword", testUser.getPassword());
        assertNotNull(testUser.getPasswordExpirationDate()); // Check if expiration date is set
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_InvalidNewPassword_ViolatesPolicy() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);

        // Expect IllegalArgumentException from PasswordPolicyValidator
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword("testuser", "oldPassword", "invalid"); // "invalid" violates policy
        });
        assertTrue(exception.getMessage().startsWith("Password policy violated:"));

        verify(passwordEncoder, never()).encode(anyString()); // Encode should not be called
        verify(userRepository, never()).save(any(User.class)); // Save should not be called
    }

    @Test
    void changePassword_UserNotFound() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword("unknownuser", "oldPassword", "newPassword");
        });
        assertEquals("User not found: unknownuser", exception.getMessage());
    }

    @Test
    void changePassword_IncorrectOldPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", "encodedOldPassword")).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword("testuser", "wrongOldPassword", "newPassword");
        });
        assertEquals("Invalid current password.", exception.getMessage());
    }

    @Test
    void changePassword_NewPasswordIsEmpty() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword("testuser", "oldPassword", "");
        });
        assertEquals("New password cannot be empty.", exception.getMessage());
    }

    @Test
    void changePassword_NewPasswordIsNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);

        // This will now be caught by PasswordPolicyValidator first if it checks for null.
        // If PasswordPolicyValidator allows null and UserService has its own check, that would be different.
        // Assuming PasswordPolicyValidator throws for null.
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword("testuser", "oldPassword", null);
        });
        // The message comes from PasswordPolicyValidator's check for null.
        assertEquals("Password cannot be null.", exception.getMessage());
    }

    // Tests for completePasswordReset with password policy validation

    @Test
    void completePasswordReset_validToken_ValidNewPassword() { // Renamed for clarity
        testUser.setResetPasswordToken("validToken");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetPasswordToken("validToken")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenService.isTokenExpired(any(LocalDateTime.class))).thenReturn(false);
        when(passwordEncoder.encode("ValidNewP@ss1")).thenReturn("encodedValidNewPassword");

        boolean result = userService.completePasswordReset("validToken", "ValidNewP@ss1");

        assertTrue(result);
        assertEquals("encodedValidNewPassword", testUser.getPassword());
        assertNull(testUser.getResetPasswordToken());
        assertNull(testUser.getResetPasswordTokenExpiry());
        assertNotNull(testUser.getPasswordExpirationDate()); // Check if expiration date is set
        verify(userRepository).save(testUser);
    }

    @Test
    void completePasswordReset_validToken_InvalidNewPassword_ViolatesPolicy() {
        testUser.setResetPasswordToken("validToken");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetPasswordToken("validToken")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenService.isTokenExpired(any(LocalDateTime.class))).thenReturn(false);
        // passwordEncoder.encode will not be called if policy validation fails

        boolean result = userService.completePasswordReset("validToken", "invalid"); // "invalid" violates policy

        assertFalse(result); // Method should return false as exception is caught
        assertEquals("encodedPassword", testUser.getPassword()); // Password should not change
        assertNotNull(testUser.getResetPasswordToken()); // Token should not be cleared on policy failure
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Tests for admin resetPassword with password policy validation
    // Existing UserService has `resetPassword(String username)` which generates password.
    // This needs to be updated to `resetPassword(String username, String newPassword)` first.
    // Assuming it has been updated as per previous subtask.

    @Test
    void adminResetPassword_userFound_ValidNewPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("ValidNewP@ss1")).thenReturn("encodedValidNewPasswordByAdmin");

        boolean result = userService.resetPassword("testuser", "ValidNewP@ss1");

        assertTrue(result);
        assertEquals("encodedValidNewPasswordByAdmin", testUser.getPassword());
        assertNotNull(testUser.getPasswordExpirationDate()); // Check if expiration date is set
        verify(userRepository).save(testUser);
    }

    @Test
    void adminResetPassword_userFound_InvalidNewPassword_ViolatesPolicy() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // passwordEncoder.encode will not be called

        boolean result = userService.resetPassword("testuser", "invalid"); // "invalid" violates policy

        assertFalse(result); // Method should return false
        assertEquals("encodedPassword", testUser.getPassword()); // Password should not change
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminResetPassword_userNotFound() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        boolean result = userService.resetPassword("unknownuser", "ValidNewP@ss1");

        assertFalse(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
