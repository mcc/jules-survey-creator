package mcc.survey.creator.dto;

import lombok.Data;

import java.util.Set;

import java.util.HashSet;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private boolean isActive;
    private Set<RoleDto> roles; // Assuming RoleDto exists
    private Set<TeamDto> teams = new HashSet<>();
}
