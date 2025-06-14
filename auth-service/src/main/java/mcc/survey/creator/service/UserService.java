package mcc.survey.creator.service;

import mcc.survey.creator.util.PasswordPolicyValidator; // Added import
import mcc.survey.creator.dto.CreateUserRequest;
import mcc.survey.creator.dto.EditUserRequest;
import mcc.survey.creator.model.User;
import mcc.survey.creator.model.Role; // Assuming Role enum/class exists
import mcc.survey.creator.repository.RoleRepository;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.exception.ResourceNotFoundException; // Assuming this exception class exists or will be created
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime; // Added for resetPasswordTokenExpiry
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
// import java.util.stream.Collectors; // Not used in the provided code

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PASSWORD_LENGTH = 12; // Example length

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private EmailService emailService;

    // Helper method to generate random password
    private String generateRandomPassword() {
        byte[] bytes = new byte[PASSWORD_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Assuming email is a new field in User model and CreateUserRequest
        // If User model doesn't have email, this check and set operation should be removed or adapted
        // For now, I'll assume User model will be updated to include an email field.
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail()); // User model has email

        // Use password from request and encode it
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        PasswordPolicyValidator.validate(request.getPassword()); // Validate password policy
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setActive(true); // Default to active

        // Map new fields
        user.setRank(request.getRank());
        user.setPost(request.getPost());
        user.setEnglishName(request.getEnglishName());
        user.setChineseName(request.getChineseName());

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null) {
            request.getRoles().forEach(roleName -> {
                try {
                    Role role = roleRepository.findByName(roleName.toUpperCase())
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role: {}", roleName);
                    // Or throw an exception if roles must be valid
                }
            });
        } else {
            Role role = roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: ROLUE_USER"));
            roles.add(role); // Make sure Role.ROLE_USER exists
        }
        user.setRoles(roles);
        user.setPasswordExpirationDate(LocalDate.now().plusDays(90));

        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", savedUser.getUsername());
        // Password is not logged.
        return savedUser;
    }

    @Transactional
    public Optional<User> editUser(Long userId, EditUserRequest request) {
        return userRepository.findById(userId).map(user -> {
            if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new RuntimeException("Error: Username is already taken!");
                }
                user.setUsername(request.getUsername());
            }
            // Assuming email is a new field in User model and EditUserRequest
            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                 if (userRepository.existsByEmail(request.getEmail())) {
                    throw new RuntimeException("Error: Email is already in use!");
                }
                user.setEmail(request.getEmail());
            }

            // Update new fields if provided
            if (request.getRank() != null) {
                user.setRank(request.getRank());
            }
            if (request.getPost() != null) {
                user.setPost(request.getPost());
            }
            if (request.getEnglishName() != null) {
                user.setEnglishName(request.getEnglishName());
            }
            if (request.getChineseName() != null) {
                user.setChineseName(request.getChineseName());
            }

            if (request.getIsActive() != null) {
                user.setActive(request.getIsActive());
            }
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                Set<Role> newRoles = new HashSet<>();
                 request.getRoles().forEach(roleName -> {
                    try {
                        Role role = roleRepository.findByName(roleName.toUpperCase())
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                    
                        newRoles.add(role);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid role: {}", roleName);
                    }
                });
                user.setRoles(newRoles);
            }
            User updatedUser = userRepository.save(user);
            log.info("Updated user: {}", updatedUser.getUsername());
            return updatedUser;
        });
    }

    @Transactional
    public boolean resetPassword(String username, String newPassword) { // Signature changed
        try {
            PasswordPolicyValidator.validate(newPassword);
        } catch (IllegalArgumentException e) {
            log.warn("Admin password reset for user {} failed due to weak password: {}", username, e.getMessage());
            return false; // Password policy violated
        }
    
        return userRepository.findByUsername(username).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordExpirationDate(LocalDate.now().plusDays(90)); // Reset expiration date
            userRepository.save(user);
            log.info("Admin successfully reset password for user: {}", username);
            // DO NOT log the newPassword itself
            return true;
        }).orElseGet(() -> {
            log.warn("User not found for admin password reset: {}", username);
            return false; // User not found
        });
    }
                     
    public void initiatePasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            // Consider throwing ResourceNotFoundException or returning a specific response
            // For now, just logging and returning to avoid leaking information about existing emails.
            return;
            // throw new ResourceNotFoundException("User not found with email: " + email);
        }

        User user = userOptional.get();
        String token = passwordResetTokenService.generateToken();
        LocalDateTime expiryDate = passwordResetTokenService.getExpiryDate();

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(expiryDate);
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
        log.info("Password reset initiated for user: {}", user.getUsername());
    }

    @Transactional
    public boolean completePasswordReset(String token, String newPassword) {
        Optional<User> userOptional = userRepository.findByResetPasswordToken(token);
        if (userOptional.isEmpty()) {
            log.warn("Invalid or non-existent password reset token used: {}", token);
            return false;
        }

        User user = userOptional.get();

        if (passwordResetTokenService.isTokenExpired(user.getResetPasswordTokenExpiry())) {
            log.warn("Expired password reset token used for user: {}", user.getUsername());
            return false;
        }

        try {
            PasswordPolicyValidator.validate(newPassword);
        } catch (IllegalArgumentException e) {
            log.warn("Password reset for user {} via token {} failed due to weak password: {}", user.getUsername(), token, e.getMessage());
            // Do not clear token here, allow user to try again with a stronger password if token still valid
            return false; // Password policy violated
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null); // Clear token after successful reset
        user.setResetPasswordTokenExpiry(null); // Clear token expiry
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", user.getUsername());
        return true;
    }

    @Transactional
    public Optional<User> setUserStatus(Long userId, boolean isActive) {
        return userRepository.findById(userId).map(user -> {
            user.setActive(isActive);
            User updatedUser = userRepository.save(user);
            log.info("Set user {} active status to: {}", user.getUsername(), isActive);
            return updatedUser;
        });
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User findByName(String name) {
        return userRepository.findByUsername(name).orElse(null);
    }

    public String createAdminUser() {
        if (userRepository.count() > 0){
            log.info("You cannot create admin account if the system already in user.");
            return "You cannot create admin account if the system already in user";
        } else {
            CreateUserRequest createUserRequest = new CreateUserRequest();
            createUserRequest.setUsername("admin");
            createUserRequest.setEmail("admin@admin.com");
            createUserRequest.setRoles(Set.of("ROLE_SYSTEM_ADMIN"));
            User user = this.createUser(createUserRequest);
            log.info("System Admin created");
            return "System Admin created";
        }
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        log.debug("Attempting to change password for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    // Consider creating a more specific exception, e.g., UserNotFoundException
                    return new RuntimeException("User not found: " + username);
                });

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Attempt to change password with incorrect current password for user: {}", username);
            // Consider creating a more specific exception, e.g., InvalidCredentialsException
            throw new RuntimeException("Invalid current password.");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            log.warn("New password cannot be empty for user: {}", username);
            throw new IllegalArgumentException("New password cannot be empty.");
        }

        // Validate new password against policy
        PasswordPolicyValidator.validate(newPassword); // Throws IllegalArgumentException if policy violated
        user.setPasswordExpirationDate(LocalDate.now().plusDays(90)); // Assuming 90 days validity
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Successfully changed password for user: {}", username);
    }
}
