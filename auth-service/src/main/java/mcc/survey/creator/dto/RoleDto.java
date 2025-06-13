package mcc.survey.creator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Long id;
    private String name;
    private Set<AuthorityDto> authorities;
}
