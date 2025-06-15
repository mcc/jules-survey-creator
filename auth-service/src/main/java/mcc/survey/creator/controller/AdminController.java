package mcc.survey.creator.controller;

import mcc.survey.creator.dto.AdminResetPasswordRequest; // Import new DTO
import mcc.survey.creator.dto.CreateUserRequest;
import mcc.survey.creator.dto.EditUserRequest;
import mcc.survey.creator.dto.UpdateUserStatusRequest;
import mcc.survey.creator.model.Role;
import mcc.survey.creator.model.User;
import mcc.survey.creator.service.UserService;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin") // Changed base path for admin functionalities
public class AdminController {

    Logger logger = org.slf4j.LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @PostConstruct
    private void init() {
        /*CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("admin");
        createUserRequest.setEmail("m@m.com");
        createUserRequest.setRoles(Set.of("ROLE_SYSTEM_ADMIN"));
        User user = userService.createUser(createUserRequest);
        System.out.println("AdminController initialized");
        */
    }


    @PostMapping("/users")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        // RuntimeExceptions from service (like "Username already taken", "Email already in use", "Role not found")
        // will be caught by GlobalExceptionHandler if they are of types like DuplicateResourceException or IllegalArgumentException.
        // Assuming userService.createUser throws appropriate exceptions.
        User newUser = userService.createUser(createUserRequest);
        // Consider returning a UserResponse DTO instead of the User entity for security/consistency
        return ResponseEntity.ok(newUser);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        // Consider returning List<UserResponse>
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        // Consider mapping to UserResponse
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> editUser(@PathVariable Long userId, @Valid @RequestBody EditUserRequest editUserRequest) {
        // RuntimeExceptions from service (like "Username already taken", "Email already in use", "Role not found")
        // will be caught by GlobalExceptionHandler if they are of types like DuplicateResourceException or IllegalArgumentException.
        // Assuming userService.editUser throws appropriate exceptions for not found or other issues.
        User updatedUser = userService.editUser(userId, editUserRequest)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId + " for update."));
        // Consider mapping to UserResponse
        return ResponseEntity.ok(updatedUser);
    }

    // Changed endpoint from /users/{username}/reset-password to /users/reset-password
    // Username is now part of the request body
    @PostMapping("/users/reset-password")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AdminResetPasswordRequest request) {
        // The userService.resetPassword now takes username and newPassword,
        // and returns false if user not found OR if password policy is violated.
        boolean result = userService.resetPassword(request.getUsername(), request.getNewPassword());
        if (result) {
            return ResponseEntity.ok("Password reset successfully for user: " + request.getUsername());
        } else {
            // Determine if failure was due to user not found or password policy.
            // For simplicity, returning a generic bad request or more specific error if userService could provide it.
            // Assuming userService logs the specific reason (user not found vs policy violation).
            // A more advanced implementation might involve custom exceptions or response objects from the service.
            // For now, a generic failure message for this controller endpoint.
            // Check if user exists first to give a more specific error for "user not found"
            if (userService.findByName(request.getUsername()) == null) {
                 throw new ResourceNotFoundException("User not found: " + request.getUsername());
            }
            // If user exists, failure is likely due to password policy (logged by service)
            // The service method already returns boolean, so a specific exception for policy failure might be better from service.
            // For now, let's assume it might throw IllegalArgumentException for policy.
            throw new IllegalArgumentException("Password reset failed for user: " + request.getUsername() + ". This could be due to a password policy violation or other server-side issue.");
        }
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> setUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest statusRequest) {
        User updatedUser = userService.setUserStatus(userId, statusRequest.getIsActive())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId + " for status update."));
        // Consider returning updated user DTO
        return ResponseEntity.ok("User status updated successfully for user ID: " + userId + ". New status: " + updatedUser.isActive());
    }
}
