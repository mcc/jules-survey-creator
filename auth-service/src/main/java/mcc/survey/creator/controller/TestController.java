package mcc.survey.creator.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import mcc.survey.creator.dto.CreateUserRequest;
import mcc.survey.creator.model.User;
import mcc.survey.creator.service.UserService;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/all")
    public String allAccess() {
        return "Public Content.";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public String userAccess() {
        return "User Content.";
    }

    @GetMapping("/useradmin")
    @PreAuthorize("hasRole('USER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public String userAdminAccess() {
        return "User Admin Board.";
    }

    @GetMapping("/systemadmin")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String systemAdminAccess() {
        return "System Admin Board.";
    }

    @Autowired
    private UserService userService;

    @GetMapping("/init-user")
    public String initUser() {
        return userService.createAdminUser();
    }
}
