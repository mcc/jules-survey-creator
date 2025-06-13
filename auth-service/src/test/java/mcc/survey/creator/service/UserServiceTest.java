package mcc.survey.creator.service;

import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedOldPassword");
    }

    @Test
    void changePassword_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        userService.changePassword("testuser", "oldPassword", "newPassword");

        verify(passwordEncoder).encode("newPassword");
        assertEquals("encodedNewPassword", testUser.getPassword());
        verify(userRepository).save(testUser);
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
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword("testuser", "oldPassword", null);
        });
        assertEquals("New password cannot be empty.", exception.getMessage());
    }
}
