package mcc.survey.creator.controller;

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
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("admin");
        createUserRequest.setEmail("m@m.com");
        createUserRequest.setRoles(Set.of(Role.ROLE_SYSTEM_ADMIN.toString()));
        User user = userService.createUser(createUserRequest);
        System.out.println("AdminController initialized");
    }


    @PostMapping("/users")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        try {
            User newUser = userService.createUser(createUserRequest);
            // Consider returning a UserResponse DTO instead of the User entity for security/consistency
            return ResponseEntity.ok(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        return userService.getUserById(userId)
                .map(ResponseEntity::ok) // Consider mapping to UserResponse
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> editUser(@PathVariable Long userId, @Valid @RequestBody EditUserRequest editUserRequest) {
        try {
            return userService.editUser(userId, editUserRequest)
                    .map(ResponseEntity::ok) // Consider mapping to UserResponse
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/{username}/reset-password")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable String username) {
        boolean result = userService.resetPassword(username);
        if (result) {
            return ResponseEntity.ok("Password reset successfully for user: " + username);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + username);
        }
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> setUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest statusRequest) {
        return userService.setUserStatus(userId, statusRequest.getIsActive())
                .map(user -> ResponseEntity.ok("User status updated successfully for user ID: " + userId + ". New status: " + user.isActive())) // Consider returning updated user DTO
                .orElse(ResponseEntity.notFound().build());
    }
}
