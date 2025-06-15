package mcc.survey.creator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedUserDTO {
    private String id; // Renamed from userId to id
    private String username;
    private String email;

    // Lombok's @AllArgsConstructor will create:
    // public SharedUserDTO(String id, String username, String email) { ... }

    // Lombok's @NoArgsConstructor will create:
    // public SharedUserDTO() { ... }

    // Lombok's @Data will create getters, setters, toString, equals, hashCode.
}
