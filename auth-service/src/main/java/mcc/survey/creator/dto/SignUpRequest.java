package mcc.survey.creator.dto;

import java.util.Set;
import mcc.survey.creator.model.Role;

public class SignUpRequest {
    private String username;
    private String password;
    private Set<String> roles; // Accepting roles as strings
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
