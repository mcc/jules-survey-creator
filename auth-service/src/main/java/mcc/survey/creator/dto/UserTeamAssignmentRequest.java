package mcc.survey.creator.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UserTeamAssignmentRequest {
    @NotEmpty
    private Set<Long> userIds;
}
