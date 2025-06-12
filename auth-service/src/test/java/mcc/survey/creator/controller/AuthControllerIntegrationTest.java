package mcc.survey.creator.controller;

import mcc.survey.creator.dto.LoginRequest;
import mcc.survey.creator.dto.SignUpRequest;
import mcc.survey.creator.model.Role;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback transactions after each test
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean up database before each test
    }

    @Test
    void registerUser_shouldCreateNewUser() throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("testuser");
        signUpRequest.setPassword("password123");
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        signUpRequest.setRoles(roles);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));

        assert(userRepository.findByUsername("testuser").isPresent());
    }

    @Test
    void registerUser_whenUsernameExists_shouldReturnBadRequest() throws Exception {
        // Arrange: Create an existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setRoles(Collections.singleton(Role.USER));
        userRepository.save(existingUser);

        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("existinguser");
        signUpRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Username is already taken!"));
    }

    @Test
    void authenticateUser_shouldReturnJwtResponse() throws Exception {
        // Arrange: Create a user
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("loginuser");
        signUpRequest.setPassword("password123");
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        signUpRequest.setRoles(roles);

        // No need to use mockMvc for setup if using repository directly
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRoles(Collections.singleton(Role.USER));
        userRepository.save(user);


        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void authenticateUser_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Spring Security default for bad credentials
    }
}
