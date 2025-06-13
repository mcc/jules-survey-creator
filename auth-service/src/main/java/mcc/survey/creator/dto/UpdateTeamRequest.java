package mcc.survey.creator.dto;

import lombok.Data;

@Data
public class UpdateTeamRequest {
    private String name;
    private String description;
    private Long serviceId;
}
