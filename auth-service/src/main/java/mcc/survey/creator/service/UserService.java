package mcc.survey.creator.service;

import mcc.survey.creator.util.PasswordPolicyValidator;
import mcc.survey.creator.dto.*; // Import all DTOs
import mcc.survey.creator.model.User;
import mcc.survey.creator.model.Role;
import mcc.survey.creator.model.Team; // Import Team model
import mcc.survey.creator.repository.RoleRepository;
import mcc.survey.creator.repository.TeamRepository; // Import TeamRepository
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.exception.ResourceNotFoundException; // Corrected import for custom exception
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
import java.util.stream.Collectors; // Will be used for mapping

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

    @Autowired
    private TeamRepository teamRepository; // Inject TeamRepository

    @Autowired
    private TeamService teamService; // Inject TeamService for mapToTeamDto

    // Helper method to generate random password
    private String generateRandomPassword() {
        byte[] bytes = new byte[PASSWORD_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // --- Helper DTO Mapping Methods ---
    public UserDto mapToUserDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setActive(user.isActive());

        if (user.getRoles() != null) {
            userDto.setRoles(user.getRoles().stream().map(role -> {
                RoleDto roleDto = new RoleDto();
                roleDto.setId(role.getId());
                roleDto.setName(role.getName());
                return roleDto;
            }).collect(Collectors.toSet()));
        }

        if (user.getTeams() != null) {
            userDto.setTeams(user.getTeams().stream()
                    .map(teamService::mapToTeamDto) // Use TeamService for mapping
                    .collect(Collectors.toSet()));
        }
        return userDto;
    }

    public List<UserDto> mapToUserDtoList(List<User> users) {
        return users.stream().map(this::mapToUserDto).collect(Collectors.toList());
    }
    // --- End Helper DTO Mapping Methods ---


    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new mcc.survey.creator.exception.DuplicateResourceException("Error: Username is already taken!");
        }

        // Assuming email is a new field in User model and CreateUserRequest
        // If User model doesn't have email, this check and set operation should be removed or adapted
        // For now, I'll assume User model will be updated to include an email field.
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new mcc.survey.creator.exception.DuplicateResourceException("Error: Email is already in use!");
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

        // Handle Team assignments
        if (request.getTeamIds() != null && !request.getTeamIds().isEmpty()) {
            Set<Team> teams = new HashSet<>();
            for (Long teamId : request.getTeamIds()) {
                Team team = teamRepository.findById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
                teams.add(team);
            }
            user.setTeams(teams);
        }

        User savedUser = userRepository.save(user);
        log.info("Created new user: {}. Generated password: {}", savedUser.getUsername(), randomPassword);
        // It's generally not a good practice to return the generated password, even in logs for long term.
        // Consider sending it via a secure channel or having user set it on first login.
        return mapToUserDto(savedUser);
    }

    @Transactional
    public UserDto editUser(Long userId, EditUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new mcc.survey.creator.exception.DuplicateResourceException("Error: Username is already taken!");
            }
            user.setUsername(request.getUsername());
        }
        // Assuming email is a new field in User model and EditUserRequest
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new mcc.survey.creator.exception.DuplicateResourceException("Error: Email is already in use!");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            request.getRoles().forEach(roleName -> {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                newRoles.add(role);
            });
            user.setRoles(newRoles);
        }

        // Handle Team assignments
        if (request.getTeamIds() != null) {
            user.getTeams().clear(); // Clear existing teams first
            if (!request.getTeamIds().isEmpty()) {
                Set<Team> newTeams = new HashSet<>();
                for (Long teamId : request.getTeamIds()) {
                    Team team = teamRepository.findById(teamId)
                            .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
                    newTeams.add(team);
                }
                user.setTeams(newTeams);
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated user: {}", updatedUser.getUsername());
        return mapToUserDto(updatedUser);
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
    public UserDto setUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setActive(isActive);
        User updatedUser = userRepository.save(user);
        log.info("Set user {} active status to: {}", user.getUsername(), isActive);
        return mapToUserDto(updatedUser);
    }

    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToUserDto(user);
    }

    public User getUserEntityById(Long userId) { // Keep a method to get Entity if needed internally
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public List<UserDto> getAllUsers() {
        return mapToUserDtoList(userRepository.findAll());
    }

    public UserDto findByName(String name) { // Changed to return UserDto
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with name: " + name));
        return mapToUserDto(user);
    }

    public String createAdminUser() {
        if (userRepository.count() > 0) {
            log.info("Admin account cannot be created if users already exist in the system.");
            return "Admin account cannot be created if users already exist in the system.";
        } else {
            CreateUserRequest createUserRequest = new CreateUserRequest();
            createUserRequest.setUsername("admin");
            createUserRequest.setEmail("admin@example.com"); // Changed to example.com
            createUserRequest.setRoles(Set.of("ROLE_SYSTEM_ADMIN"));
            // No teams for initial admin
            UserDto userDto = this.createUser(createUserRequest); // createUser now returns UserDto
            log.info("System Admin created: {}", userDto.getUsername());
            return "System Admin created: " + userDto.getUsername();
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
