package mcc.survey.creator.controller;

import mcc.survey.creator.dto.JwtResponse;
import mcc.survey.creator.dto.LoginRequest;
import mcc.survey.creator.dto.RefreshTokenRequest;
import mcc.survey.creator.dto.SignUpRequest;
import mcc.survey.creator.model.Role;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
        import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

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
            roles.add(Role.USER); // Default role
        } else {
            strRoles.forEach(role -> {
                switch (role.toUpperCase()) {
                    case "SYSTEM_ADMIN":
                        roles.add(Role.SYSTEM_ADMIN);
                        break;
                    case "USER_ADMIN":
                        roles.add(Role.USER_ADMIN);
                        break;
                    default:
                        roles.add(Role.USER);
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
}
