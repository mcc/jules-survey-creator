package mcc.survey.creator.controller;

import mcc.survey.creator.dto.*; // Import all DTOs
import mcc.survey.creator.exception.DuplicateResourceException;
import mcc.survey.creator.exception.ResourceNotFoundException;
import mcc.survey.creator.service.ServiceService;
import mcc.survey.creator.service.TeamService;
import mcc.survey.creator.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // Keep @Valid
import java.util.List;
import java.util.Set; // Keep Set if used for UserDto.teams

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class); // Correct logger initialization

    private final UserService userService;
    private final ServiceService serviceService;
    private final TeamService teamService;

    @Autowired
    public AdminController(UserService userService, ServiceService serviceService, TeamService teamService) {
        this.userService = userService;
        this.serviceService = serviceService;
        this.teamService = teamService;
    }

    // @PostConstruct removed as it was for testing/init user, which should be handled by data.sql or a CommandLineRunner
    // private void init() {
    //    /*CreateUserRequest createUserRequest = new CreateUserRequest();
    //    createUserRequest.setUsername("admin");
    //    createUserRequest.setEmail("m@m.com");
    //    createUserRequest.setRoles(Set.of("ROLE_SYSTEM_ADMIN"));
    //    UserDto user = userService.createUser(createUserRequest); // userService now returns Dto
    //    System.out.println("AdminController initialized");
    //    */
    // }

    // --- User Management Endpoints ---
    @PostMapping("/users")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        try {
            UserDto newUserDto = userService.createUser(createUserRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUserDto);
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ResourceNotFoundException e) { // For invalid roles/teams
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error creating user: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            UserDto userDto = userService.getUserById(userId);
            return ResponseEntity.ok(userDto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> editUser(@PathVariable Long userId, @Valid @RequestBody EditUserRequest editUserRequest) {
        try {
            UserDto updatedUserDto = userService.editUser(userId, editUserRequest);
            return ResponseEntity.ok(updatedUserDto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error editing user {}: ", userId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/reset-password")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AdminResetPasswordRequest request) {
        try {
            boolean result = userService.resetPassword(request.getUsername(), request.getNewPassword());
            if (result) {
                return ResponseEntity.ok("Password reset successfully for user: " + request.getUsername());
            } else {
                 // This path might be hit if password policy fails within resetPassword before user check
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password reset failed. Check password policy or user existence.");
            }
        } catch (ResourceNotFoundException e) { // If findByName in service throws it
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) { // For password policy violations
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> setUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest statusRequest) {
        try {
            UserDto userDto = userService.setUserStatus(userId, statusRequest.getIsActive());
            return ResponseEntity.ok(userDto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // --- Service Management Endpoints ---
    @PostMapping("/services")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> createService(@Valid @RequestBody CreateServiceRequest request) {
        try {
            ServiceDto createdService = serviceService.createService(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/services")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        return ResponseEntity.ok(serviceService.getAllServices());
    }

    @GetMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> getServiceById(@PathVariable Long serviceId) {
        try {
            ServiceDto serviceDto = serviceService.getServiceById(serviceId);
            return ResponseEntity.ok(serviceDto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> updateService(@PathVariable Long serviceId, @Valid @RequestBody UpdateServiceRequest request) {
        try {
            ServiceDto updatedService = serviceService.updateService(serviceId, request);
            return ResponseEntity.ok(updatedService);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long serviceId) {
        try {
            serviceService.deleteService(serviceId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DuplicateResourceException e) { // If service has teams
             return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // Or a message
        }
    }

    // --- Team Management Endpoints ---
    @PostMapping("/teams")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        try {
            TeamDto createdTeam = teamService.createTeam(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
        } catch (ResourceNotFoundException e) { // For service not found
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/teams")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<TeamDto>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/services/{serviceId}/teams")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> getTeamsByService(@PathVariable Long serviceId) {
        try {
            // Validate service exists first by calling serviceService or let teamService handle it.
            // teamService.getTeamsByService already throws ResourceNotFoundException if service not found.
            List<TeamDto> teams = teamService.getTeamsByService(serviceId);
            return ResponseEntity.ok(teams);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/teams/{teamId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> getTeamById(@PathVariable Long teamId) {
        try {
            TeamDto teamDto = teamService.getTeamById(teamId);
            return ResponseEntity.ok(teamDto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/teams/{teamId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> updateTeam(@PathVariable Long teamId, @Valid @RequestBody UpdateTeamRequest request) {
        try {
            TeamDto updatedTeam = teamService.updateTeam(teamId, request);
            return ResponseEntity.ok(updatedTeam);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // Team or new Service not found
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/teams/{teamId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId) {
        try {
            teamService.deleteTeam(teamId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // --- Team Membership Endpoints ---
    @PostMapping("/teams/{teamId}/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> assignUsersToTeam(@PathVariable Long teamId, @Valid @RequestBody UserTeamAssignmentRequest request) {
        try {
            TeamDto updatedTeam = teamService.assignUsersToTeam(teamId, request.getUserIds());
            return ResponseEntity.ok(updatedTeam);
        } catch (ResourceNotFoundException e) { // Team or any User not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/teams/{teamId}/users/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> removeUserFromTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        try {
            TeamDto updatedTeam = teamService.removeUserFromTeam(teamId, userId);
            return ResponseEntity.ok(updatedTeam);
        } catch (ResourceNotFoundException e) { // Team or User not found, or User not in Team
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/teams/{teamId}/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> getUsersInTeam(@PathVariable Long teamId) {
        try {
            Set<UserDto> users = teamService.getUsersInTeam(teamId);
            return ResponseEntity.ok(users);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
