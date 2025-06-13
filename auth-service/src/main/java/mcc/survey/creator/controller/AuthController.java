package mcc.survey.creator.controller;

import mcc.survey.creator.dto.JwtResponse;
import mcc.survey.creator.dto.LoginRequest;
import mcc.survey.creator.dto.RefreshTokenRequest;
import mcc.survey.creator.dto.SignUpRequest;
import mcc.survey.creator.model.Role;
import mcc.survey.creator.dto.ChangePasswordRequest; // New DTO
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.security.JwtTokenProvider;
import mcc.survey.creator.service.UserService; // Autowire this
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

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()") // Ensure user is authenticated
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getPrincipal().toString();
        }

        boolean passwordChanged = userService.changePassword(
            username,
            changePasswordRequest.getOldPassword(),
            changePasswordRequest.getNewPassword()
        );

        if (passwordChanged) {
            return ResponseEntity.ok("Password changed successfully.");
        } else {
            // Consider more specific error messages based on UserService's return or exceptions
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Could not change password. Check old password or user status.");
        }
    }
}
