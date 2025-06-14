package mcc.survey.creator.dto;

import lombok.Data;
import java.util.Set;
import java.util.HashSet;

@Data
public class EditUserRequest {
    private String username;
    private String email;
    private Set<String> roles;
    private Boolean isActive; // Use Boolean to allow for optional updates
    private Set<Long> teamIds = new HashSet<>();
}
