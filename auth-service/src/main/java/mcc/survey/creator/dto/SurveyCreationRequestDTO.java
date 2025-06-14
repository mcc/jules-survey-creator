package mcc.survey.creator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import mcc.survey.creator.model.User; // Assuming User might be needed for owner, adjust if not.
                                      // It's likely not needed directly in DTO if owner is handled by controller.

import java.sql.Timestamp; // For createdAt, updatedAt
import java.util.Date;    // For creationDate, modificationDate
import java.util.HashSet; // For sharedWithUsers
import java.util.Set;     // For sharedWithUsers

@Data
public class SurveyCreationRequestDTO {

    private String title;
    private String description;
    private JsonNode surveyJson; // Key change: Accept JsonNode
    private String surveyMode;
    // private User owner; // Owner will be set by the controller based on authenticated user
    private String dataClassification;
    private String status;
    // Timestamps like createdAt, updatedAt, creationDate, modificationDate are usually set by the server/JPA
    // private Timestamp createdAt;
    // private Timestamp updatedAt;
    // private Date creationDate;
    // private Date modificationDate;

    // sharedWithUsers is also likely handled post-creation or by specific endpoints
    // private Set<User> sharedWithUsers = new HashSet<>();

    // Add constructors, getters, and setters if not using Lombok, or ensure Lombok is configured.
    // Lombok @Data should handle these.
}
