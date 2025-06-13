package mcc.survey.creator.controller;

import mcc.survey.creator.dto.JwtResponse;
import mcc.survey.creator.dto.LoginRequest;
import mcc.survey.creator.dto.MessageResponseDto; // Added import
import mcc.survey.creator.dto.*; // Import all DTOs
import mcc.survey.creator.model.Role;
import mcc.survey.creator.dto.ChangePasswordRequest; // New DTO
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.security.JwtTokenProvider;
import mcc.survey.creator.service.UserService; // Autowire this
import mcc.survey.creator.util.ResourceNotFoundException; // Import ResourceNotFoundException
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth") // Base path for auth related endpoints
public class AuthController { // Renaming to UserController or creating a new one might be better for non-auth user ops

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            Optional<User> userOptional = userRepository.findByUsername(principal.getUsername());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPasswordExpirationDate() != null &&
                    (user.getPasswordExpirationDate().isBefore(LocalDate.now()) || user.getPasswordExpirationDate().isEqual(LocalDate.now()))) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Your password has expired. Please change your password.");
                }
            } else {
                // This case should ideally not happen if authentication was successful
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: User details not found after authentication.");
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            return ResponseEntity.ok(new JwtResponse(jwt, refreshToken));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequest signUpRequest) {
        if (userRepository.findByUsername(signUpRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(new Role("ROLE_USER")); // Default role
        } else {
            strRoles.forEach(role -> {
                switch (role.toUpperCase()) {
                    case "ROLE_SYSTEM_ADMIN":
                        roles.add(new Role("ROLE_SYSTEM_ADMIN"));
                        break;
                    case "ROLE_USER_ADMIN":
                        roles.add(new Role("ROLE_USER_ADMIN"));
                        break;
                    default:
                        roles.add(new Role("ROLE_USER"));
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String requestRefreshToken = refreshTokenRequest.getRefreshToken();

        if (jwtTokenProvider.validateToken(requestRefreshToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(requestRefreshToken);
            String newAccessToken = jwtTokenProvider.generateToken(authentication);
            // Optionally, generate a new refresh token as well if you want refresh token rotation
            // String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            // return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken));
            return ResponseEntity.ok(new JwtResponse(newAccessToken, requestRefreshToken));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // For JWT, logout is typically handled on the client side by deleting the token.
        // If you need server-side stateful logout (e.g., token blacklisting),
        // you would implement that logic here.
        // For this example, we'll just return a success message.
        SecurityContextHolder.clearContext(); // Clear server-side security context
        return ResponseEntity.ok("User logged out successfully!");
    }

    // Simple DTO for returning basic user info
    static class UserSummaryDTO {
        private Long id;
        private String username;

        public UserSummaryDTO(Long id, String username) {
            this.id = id;
            this.username = username;
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
    }

    @GetMapping("/users/by-username/{username}") // Changed path to be under /api/auth/users for now
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return ResponseEntity.ok(new UserSummaryDTO(user.getId(), user.getUsername()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        try {
            userService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok("Password reset email sent. Please check your inbox.");
        } catch (ResourceNotFoundException e) {
            // Even if user is not found, we might want to return a generic success message
            // to prevent email enumeration attacks.
            // However, the current userService.initiatePasswordReset logs and returns void if not found.
            // For more explicit client feedback (and if not concerned about enumeration):
            // return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            return ResponseEntity.ok("If your email is registered, you will receive a password reset link.");
        } catch (Exception e) {
            // Log the exception e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error initiating password reset.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        if (request.getToken() == null || request.getToken().isEmpty() ||
            request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ResponseEntity.badRequest().body("Token and new password must be provided.");
        }

        boolean result = userService.completePasswordReset(request.getToken(), request.getNewPassword());

        if (result) {
            return ResponseEntity.ok("Password has been reset successfully.");
        } else {
            // More specific errors could be returned from the service layer if needed
            return ResponseEntity.badRequest().body("Invalid or expired token, or password could not be reset.");
        }
    }

    // Ensure this is the version of changePassword to keep.
    // It is mapped to /api/auth/users/change-password
    // It calls the userService.changePassword variant that takes three arguments (username, oldPassword, newPassword)
    // And is designed to throw exceptions for specific error conditions.
    @PostMapping("/users/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest, Authentication authentication) { // Added @Valid
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: User is not authenticated.");
        }
        String username = authentication.getName();

        // The @Valid annotation on ChangePasswordRequest should handle cases like empty newPassword
        // if the DTO has appropriate validation annotations (e.g., @NotEmpty, @Size).
        // If ChangePasswordRequest DTO doesn't have such annotations, the explicit null/empty check can remain,
        // or preferably, add validation annotations to the DTO.
        // For now, assuming @Valid might not cover all desired checks or DTO is not yet annotated for this.
        if (changePasswordRequest.getNewPassword() == null || changePasswordRequest.getNewPassword().isEmpty()) {
             return ResponseEntity.badRequest().body("Error: New password cannot be empty.");
        }
        // Example of an additional check (can be removed if DTO validation is comprehensive)
        // if (changePasswordRequest.getOldPassword() == null || changePasswordRequest.getOldPassword().isEmpty()) {
        //     return ResponseEntity.badRequest().body("Error: Old password cannot be empty.");
        // }

        try {
            userService.changePassword(username, changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword());
            return ResponseEntity.ok(new MessageResponseDto("Password changed successfully.")); // Using a DTO for response consistency
        } catch (ResourceNotFoundException e) { // Catching specific expected exceptions
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponseDto("Error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponseDto("Error: " + e.getMessage()));
        } catch (RuntimeException e) { // Catch-all for other unexpected runtime exceptions
            // Log the exception server-side for more details
            // log.error("Error changing password for user {}: {}", username, e.getMessage(), e); // Example logging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponseDto("Error: An unexpected error occurred while changing password."));
        }
      
      
    }
}
