package mcc.survey.creator.dto;

import lombok.Data;
import java.util.Set;
import java.util.HashSet;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password; // Assuming password is required for creation
    private Set<String> roles;
    private Set<Long> teamIds = new HashSet<>();
}
