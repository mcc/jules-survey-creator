package mcc.survey.creator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTeamRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Long serviceId;
}
