package mcc.survey.creator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mcc.survey.creator.dto.ForgotPasswordRequestDto;
import mcc.survey.creator.dto.ResetPasswordRequestDto;
import mcc.survey.creator.service.UserService;
import mcc.survey.creator.util.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Specify AuthController to ensure only it is loaded, not all controllers.
@WebMvcTest(AuthController.class)
class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // Mock other dependencies of AuthController if they are used by the tested endpoints
    // and not already mocked by @WebMvcTest (e.g. AuthenticationManager, JwtTokenProvider etc. are part of spring security)
    // For these specific password reset endpoints, only UserService seems directly involved from AuthController's own code.
    // Spring Security related beans like AuthenticationManager, UserRepository, PasswordEncoder, JwtTokenProvider
    // are typically part of the security filter chain which might be auto-configured.
    // If tests fail due to missing beans for security setup, we might need @MockBean for them or adjust test config.
    // However, for these specific endpoints, they might not trigger full authentication flows.

    @Autowired
    private ObjectMapper objectMapper; // For converting DTOs to JSON

    @Test
    void forgotPassword_validEmail_shouldReturnOk() throws Exception {
        ForgotPasswordRequestDto requestDto = new ForgotPasswordRequestDto();
        requestDto.setEmail("test@example.com");

        doNothing().when(userService).initiatePasswordReset(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset email sent. Please check your inbox."));

        verify(userService).initiatePasswordReset("test@example.com");
    }

    @Test
    void forgotPassword_emailNotFound_shouldReturnOkWithGenericMessage() throws Exception {
        ForgotPasswordRequestDto requestDto = new ForgotPasswordRequestDto();
        requestDto.setEmail("notfound@example.com");

        // Current AuthController implementation catches ResourceNotFoundException
        // and returns a generic success message.
        doThrow(new ResourceNotFoundException("User not found"))
            .when(userService).initiatePasswordReset("notfound@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("If your email is registered, you will receive a password reset link."));

        verify(userService).initiatePasswordReset("notfound@example.com");
    }

    @Test
    void forgotPassword_serviceThrowsOtherException_shouldReturnInternalServerError() throws Exception {
        ForgotPasswordRequestDto requestDto = new ForgotPasswordRequestDto();
        requestDto.setEmail("error@example.com");

        doThrow(new RuntimeException("Unexpected service error"))
            .when(userService).initiatePasswordReset("error@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error initiating password reset."));

        verify(userService).initiatePasswordReset("error@example.com");
    }


    @Test
    void resetPassword_validTokenAndPassword_shouldReturnOk() throws Exception {
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setToken("validToken");
        requestDto.setNewPassword("newStrongPassword");

        when(userService.completePasswordReset(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password has been reset successfully."));

        verify(userService).completePasswordReset("validToken", "newStrongPassword");
    }

    @Test
    void resetPassword_invalidOrExpiredToken_shouldReturnBadRequest() throws Exception {
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setToken("invalidToken");
        requestDto.setNewPassword("newStrongPassword");

        when(userService.completePasswordReset(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired token, or password could not be reset."));

        verify(userService).completePasswordReset("invalidToken", "newStrongPassword");
    }

    @Test
    void resetPassword_missingToken_shouldReturnBadRequest() throws Exception {
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        // requestDto.setToken(null); // Token is null
        requestDto.setNewPassword("newStrongPassword");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token and new password must be provided."));

        verify(userService, never()).completePasswordReset(anyString(), anyString());
    }

    @Test
    void resetPassword_missingPassword_shouldReturnBadRequest() throws Exception {
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setToken("aToken");
        // requestDto.setNewPassword(null); // Password is null

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token and new password must be provided."));

        verify(userService, never()).completePasswordReset(anyString(), anyString());
    }
}
