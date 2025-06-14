package mcc.survey.creator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateServiceRequest {
    @NotBlank
    private String name;
    private String description;
}
