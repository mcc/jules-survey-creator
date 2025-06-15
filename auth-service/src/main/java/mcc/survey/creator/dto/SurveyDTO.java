package mcc.survey.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDTO {
    private Long id;
    private String title;
    private String description;
    private JsonNode surveyJson;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String surveyMode;
    private String dataClassification;
    private String status;
    private UserDTO owner;
    private Set<UserDTO> sharedWithUsers;
}
