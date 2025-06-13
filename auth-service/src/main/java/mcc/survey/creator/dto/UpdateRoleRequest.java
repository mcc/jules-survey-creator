package mcc.survey.creator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateRoleRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String name;
    private Set<Long> authorityIds; // IDs of authorities to assign
}
