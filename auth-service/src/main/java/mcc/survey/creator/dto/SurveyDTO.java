package mcc.survey.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDTO {
    private Long id;
    private String title;
    private String description;
    private com.fasterxml.jackson.databind.JsonNode surveyJson;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String surveyMode;
    private String dataClassification;
    private String status;
    private UserDTO owner;
    private Set<UserDTO> sharedWithUsers;
}
