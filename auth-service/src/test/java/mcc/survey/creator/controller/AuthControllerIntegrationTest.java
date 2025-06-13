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
import java.time.LocalDate; // Added for password expiration
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.is; // Added for jsonPath value checking
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

    @Autowired
    private mcc.survey.creator.repository.RoleRepository roleRepository; // Added RoleRepository

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean up users

        // Ensure essential roles exist, create if not
        findOrCreateRole("ROLE_USER");
        findOrCreateRole("ROLE_USER_ADMIN");
        findOrCreateRole("ROLE_SYSTEM_ADMIN");
    }

    private Role findOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role(roleName);
                    return roleRepository.save(newRole);
                });
    }

    @Test
    void registerUser_shouldCreateNewUser() throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("testuser");
        signUpRequest.setPassword("password123");
        Set<String> strRoles = new HashSet<>();
        strRoles.add("ROLE_USER"); // Assuming "ROLE_USER" is one of the roles set by DataInitializer
        signUpRequest.setRoles(strRoles);

        // Ensure ROLE_USER exists from DataInitializer, or create it if not for test setup resilience
        // Role userRole = roleRepository.findByName("ROLE_USER")
        //         .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        // No need to fetch userRole here for signup, controller handles it or gets if from request

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));

        User savedUser = userRepository.findByUsername("testuser").orElseThrow();
        assert(savedUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_USER"))); // ROLE_USER is default
    }

    @Test
    void registerUser_whenUsernameExists_shouldReturnBadRequest() throws Exception {
        // Arrange: Create an existing user
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setRoles(Collections.singleton(userRole));
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
        roles.add("ROLE_USER");
        signUpRequest.setRoles(roles);

        // No need to use mockMvc for setup if using repository directly
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        user.setRoles(Collections.singleton(userRole));
        user.setPasswordExpirationDate(LocalDate.now().plusDays(1)); // Ensure not expired for this test
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

    @Test
    void whenLoginWithExpiredPassword_thenUnauthorized() throws Exception {
        // Arrange: Create a user with an expired password
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        User expiredUser = new User();
        expiredUser.setUsername("expireduser");
        expiredUser.setPassword(passwordEncoder.encode("password123"));
        expiredUser.setRoles(Collections.singleton(userRole));
        expiredUser.setEmail("expired@example.com"); // Ensure email is not null
        expiredUser.setActive(true);
        expiredUser.setPasswordExpirationDate(LocalDate.now().minusDays(1)); // Expired
        userRepository.save(expiredUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("expireduser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Error: Your password has expired. Please change your password."));
    }

    @Test
    void whenLoginWithNonExpiredPassword_thenOk() throws Exception {
        // Arrange: Create a user with a non-expired password
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        User validUser = new User();
        validUser.setUsername("validuser");
        validUser.setPassword(passwordEncoder.encode("password123"));
        validUser.setRoles(Collections.singleton(userRole));
        validUser.setEmail("valid@example.com"); // Ensure email is not null
        validUser.setActive(true);
        validUser.setPasswordExpirationDate(LocalDate.now().plusDays(30)); // Not expired
        userRepository.save(validUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("validuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
