package mcc.survey.creator.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
