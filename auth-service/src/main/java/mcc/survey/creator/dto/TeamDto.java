package mcc.survey.creator.dto;

import lombok.Data;

@Data
public class TeamDto {
    private Long id;
    private String name;
    private String description;
    private Long serviceId;
    private String serviceName;
}
