package mcc.survey.creator.service;

import mcc.survey.creator.dto.*;
import mcc.survey.creator.exception.DuplicateResourceException;
import mcc.survey.creator.exception.ResourceNotFoundException;
import mcc.survey.creator.model.Service;
import mcc.survey.creator.model.Team;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.TeamRepository;
import mcc.survey.creator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final ServiceService serviceService; // Use the ServiceService to get Service entity
    private final UserRepository userRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository, ServiceService serviceService, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.serviceService = serviceService;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeamDto createTeam(CreateTeamRequest request) {
        Service service = serviceService.getServiceEntityById(request.getServiceId());
        if (teamRepository.existsByNameAndService(request.getName(), service)) {
            throw new DuplicateResourceException("Team with name '" + request.getName() + "' already exists in service '" + service.getName() + "'.");
        }
        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setService(service);
        return mapToTeamDto(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public TeamDto getTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .map(this::mapToTeamDto)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    @Transactional(readOnly = true)
    public Team getTeamEntityById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return mapToTeamDtoList(teamRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getTeamsByService(Long serviceId) {
        Service service = serviceService.getServiceEntityById(serviceId);
        return mapToTeamDtoList(teamRepository.findByService(service));
    }

    @Transactional
    public TeamDto updateTeam(Long teamId, UpdateTeamRequest request) {
        Team team = getTeamEntityById(teamId);
        Service service = team.getService();

        if (request.getServiceId() != null && !request.getServiceId().equals(service.getId())) {
            service = serviceService.getServiceEntityById(request.getServiceId());
            team.setService(service);
        }

        if (request.getName() != null && !request.getName().equals(team.getName())) {
            if (teamRepository.existsByNameAndService(request.getName(), service)) {
                throw new DuplicateResourceException("Team with name '" + request.getName() + "' already exists in service '" + service.getName() + "'.");
            }
            team.setName(request.getName());
        }

        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        return mapToTeamDto(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = getTeamEntityById(teamId);
        // Consider implications: users in team. For now, just remove associations.
        team.getUsers().forEach(user -> user.getTeams().remove(team));
        userRepository.saveAll(team.getUsers()); // Save users to update their team associations
        team.getUsers().clear(); // Clear users from team side
        teamRepository.save(team); // Persist changes to team (empty user set)
        teamRepository.delete(team);
    }

    @Transactional
    public TeamDto assignUsersToTeam(Long teamId, Set<Long> userIds) {
        Team team = getTeamEntityById(teamId);
        Set<User> usersToAssign = new HashSet<>();
        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            usersToAssign.add(user);
        }

        usersToAssign.forEach(user -> {
            user.getTeams().add(team); // Add team to user's set of teams
            team.getUsers().add(user); // Add user to team's set of users
        });

        userRepository.saveAll(usersToAssign); // Persist changes to users
        return mapToTeamDto(teamRepository.save(team)); // Persist changes to team
    }

    @Transactional
    public TeamDto removeUserFromTeam(Long teamId, Long userId) {
        Team team = getTeamEntityById(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!team.getUsers().contains(user)) {
            throw new ResourceNotFoundException("User with id '" + userId + "' is not part of team '" + team.getName() + "'.");
        }

        team.getUsers().remove(user);
        user.getTeams().remove(team);

        userRepository.save(user); // Persist change to user
        return mapToTeamDto(teamRepository.save(team)); // Persist change to team
    }

    @Transactional(readOnly = true)
    public Set<UserDto> getUsersInTeam(Long teamId) {
        Team team = getTeamEntityById(teamId);
        return team.getUsers().stream().map(user -> {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setActive(user.isActive());
            // Add other relevant fields from User to UserDto as needed
            // For example, if roles/teams are needed as strings or simplified DTOs:
            // dto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
            return dto;
        }).collect(Collectors.toSet());
    }

    public TeamDto mapToTeamDto(Team team) {
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        if (team.getService() != null) {
            dto.setServiceId(team.getService().getId());
            dto.setServiceName(team.getService().getName());
        }
        return dto;
    }

    public List<TeamDto> mapToTeamDtoList(List<Team> teams) {
        return teams.stream().map(this::mapToTeamDto).collect(Collectors.toList());
    }
}
