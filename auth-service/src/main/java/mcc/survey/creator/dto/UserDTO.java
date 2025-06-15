package mcc.survey.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
import java.util.Set;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String rank;
    private String post;
    private String englishName;
    private String chineseName;
    private Set<String> roles;
    private boolean isActive;
}
