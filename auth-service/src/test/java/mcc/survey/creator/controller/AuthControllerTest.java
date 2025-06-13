package mcc.survey.creator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mcc.survey.creator.dto.ChangePasswordRequest;
import mcc.survey.creator.security.JwtTokenProvider;
import mcc.survey.creator.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser; // Required for @PreAuthorize
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider; // Mocked as it's a dependency of AuthController

    @MockBean
    private AuthenticationManager authenticationManager; // Mocked

    // @MockBean // UserRepository might not be needed if AuthController only uses UserService for user ops
    // private mcc.survey.creator.repository.UserRepository userRepository;


    @Test
    @WithMockUser(username = "testuser") // Provides mock Authentication for @PreAuthorize
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        doNothing().when(userService).changePassword("testuser", "oldPass", "newPass");

        mockMvc.perform(post("/api/auth/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully."));

        verify(userService).changePassword("testuser", "oldPass", "newPass");
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_InvalidOldPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongOldPass");
        request.setNewPassword("newPass");

        doThrow(new RuntimeException("Invalid current password."))
                .when(userService).changePassword("testuser", "wrongOldPass", "newPass");

        mockMvc.perform(post("/api/auth/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error: Invalid current password."));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_UserNotFound() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        doThrow(new RuntimeException("User not found: testuser"))
            .when(userService).changePassword("testuser", "oldPass", "newPass");

        mockMvc.perform(post("/api/auth/users/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Error: User not found: testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_NewPasswordEmptyInRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword(""); // Empty new password

        // This test assumes the controller's own validation catches this first.
        // If the validation is primarily in the service, then mock userService to throw IllegalArgumentException.
        // The current AuthController code has a direct check for empty new password.

        mockMvc.perform(post("/api/auth/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error: New password cannot be empty."));

        verifyNoInteractions(userService); // userService.changePassword should not be called
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_NewPasswordNullInRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword(null); // Null new password

        mockMvc.perform(post("/api/auth/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error: New password cannot be empty."));

        verifyNoInteractions(userService);
    }

    // Test for unauthenticated access
    @Test
    void changePassword_Unauthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        // Note: @WithMockUser is not used here, so the request is anonymous.
        // Spring Security by default will deny access, often with a 401 or 403.
        // For @PreAuthorize, it's typically a 401 if not authenticated.
        mockMvc.perform(post("/api/auth/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // Or .isForbidden() depending on exact config
    }
}
