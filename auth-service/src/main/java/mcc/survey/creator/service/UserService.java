package mcc.survey.creator.service;

import mcc.survey.creator.dto.CreateUserRequest;
import mcc.survey.creator.dto.EditUserRequest;
import mcc.survey.creator.model.User;
import mcc.survey.creator.model.Role; // Assuming Role enum/class exists
import mcc.survey.creator.repository.RoleRepository;
import mcc.survey.creator.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
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
        user.setEmail(request.getEmail()); // Uncomment if User model has email

        String randomPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setActive(true); // Default to active

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
        log.info("Created new user: {}. Generated password: {}", savedUser.getUsername(), randomPassword);
        // It's generally not a good practice to return the generated password, even in logs for long term.
        // Consider sending it via a secure channel or having user set it on first login.
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
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) {
            log.warn("User not found for password change: {}", username);
            return false; // Or throw exception
        }
        User user = userOptional.get();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Old password does not match for user: {}", username);
            return false; // Or throw exception indicating incorrect old password
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // IMPORTANT: Update the password expiration date upon successful password change
        user.setPasswordExpirationDate(LocalDate.now().plusDays(90)); // Assuming 90 days validity
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", username);
        return true;
    }

    @Transactional
    public boolean resetPassword(String username) {
        return userRepository.findByUsername(username).map(user -> {
            String newPassword = generateRandomPassword();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordExpirationDate(LocalDate.now().plusDays(90));
            userRepository.save(user);
            log.info("Reset password for user: {}. New password: {}", username, newPassword);
            // Again, logging passwords is risky.
            return true;
        }).orElseGet(() -> {
            log.warn("User not found for password reset: {}", username);
            return false;
        });
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
            createUserRequest.setEmail(null);
            createUserRequest.setRoles(Set.of("ROLE_SYSTEM_ADMIN"));
            User user = this.createUser(createUserRequest);
            log.info("System Admin created");
            return "System Admin created";
        }
    }
}
