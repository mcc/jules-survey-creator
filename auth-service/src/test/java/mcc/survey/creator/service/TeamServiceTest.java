package mcc.survey.creator.service;

import mcc.survey.creator.dto.*;
import mcc.survey.creator.exception.DuplicateResourceException;
import mcc.survey.creator.exception.ResourceNotFoundException;
import mcc.survey.creator.model.Service;
import mcc.survey.creator.model.Team;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.TeamRepository;
import mcc.survey.creator.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ServiceService serviceService; // Using ServiceService to get Service entity

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    private Service service;
    private Team team;
    private TeamDto teamDto;
    private User user1;
    private User user2;
    private CreateTeamRequest createTeamRequest;
    private UpdateTeamRequest updateTeamRequest;

    @BeforeEach
    void setUp() {
        service = new Service();
        service.setId(1L);
        service.setName("Test Service");

        team = new Team();
        team.setId(1L);
        team.setName("Test Team");
        team.setDescription("Test Team Description");
        team.setService(service);
        team.setUsers(new HashSet<>());

        teamDto = new TeamDto();
        teamDto.setId(1L);
        teamDto.setName("Test Team");
        teamDto.setDescription("Test Team Description");
        teamDto.setServiceId(service.getId());
        teamDto.setServiceName(service.getName());

        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setTeams(new HashSet<>());

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setTeams(new HashSet<>());

        createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setName("New Team");
        createTeamRequest.setDescription("New Team Desc");
        createTeamRequest.setServiceId(1L);

        updateTeamRequest = new UpdateTeamRequest();
        updateTeamRequest.setName("Updated Team Name");
        updateTeamRequest.setDescription("Updated Team Desc");
        updateTeamRequest.setServiceId(1L); // Can also test changing serviceId
    }

    @Test
    void createTeam_success() {
        when(serviceService.getServiceEntityById(1L)).thenReturn(service);
        when(teamRepository.existsByNameAndService(createTeamRequest.getName(), service)).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team savedTeam = invocation.getArgument(0);
            savedTeam.setId(2L); // Simulate saving and getting an ID
            return savedTeam;
        });

        TeamDto result = teamService.createTeam(createTeamRequest);

        assertNotNull(result);
        assertEquals(createTeamRequest.getName(), result.getName());
        assertEquals(service.getId(), result.getServiceId());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void createTeam_serviceNotFound_throwsResourceNotFoundException() {
        when(serviceService.getServiceEntityById(1L)).thenThrow(new ResourceNotFoundException("Service not found"));

        assertThrows(ResourceNotFoundException.class, () -> teamService.createTeam(createTeamRequest));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_duplicateNameInService_throwsDuplicateResourceException() {
        when(serviceService.getServiceEntityById(1L)).thenReturn(service);
        when(teamRepository.existsByNameAndService(createTeamRequest.getName(), service)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teamService.createTeam(createTeamRequest));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void getTeamById_success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        TeamDto result = teamService.getTeamById(1L);
        assertNotNull(result);
        assertEquals(team.getName(), result.getName());
    }

    @Test
    void getTeamEntityById_success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        Team result = teamService.getTeamEntityById(1L);
        assertNotNull(result);
        assertEquals(team.getName(), result.getName());
    }

    @Test
    void getTeamById_notFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.getTeamById(1L));
    }

    @Test
    void getTeamEntityById_notFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.getTeamEntityById(1L));
    }


    @Test
    void getAllTeams_success() {
        when(teamRepository.findAll()).thenReturn(List.of(team));
        List<TeamDto> result = teamService.getAllTeams();
        assertEquals(1, result.size());
        assertEquals(teamDto.getName(), result.get(0).getName());
    }

    @Test
    void getTeamsByService_success() {
        when(serviceService.getServiceEntityById(1L)).thenReturn(service);
        when(teamRepository.findByService(service)).thenReturn(List.of(team));

        List<TeamDto> result = teamService.getTeamsByService(1L);

        assertEquals(1, result.size());
        assertEquals(teamDto.getName(), result.get(0).getName());
    }

    @Test
    void getTeamsByService_serviceNotFound_throwsResourceNotFoundException() {
        when(serviceService.getServiceEntityById(1L)).thenThrow(new ResourceNotFoundException("Service not found"));
        assertThrows(ResourceNotFoundException.class, () -> teamService.getTeamsByService(1L));
    }

    @Test
    void updateTeam_success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        // Assuming service ID doesn't change in this specific test case for simplicity
        // If serviceId changes, serviceService.getServiceEntityById(newServiceId) would also need mocking
        when(teamRepository.existsByNameAndService(updateTeamRequest.getName(), service)).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamDto result = teamService.updateTeam(1L, updateTeamRequest);

        assertNotNull(result);
        assertEquals(updateTeamRequest.getName(), result.getName());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void updateTeam_changeService_success() {
        Service newService = new Service();
        newService.setId(2L);
        newService.setName("New Service");
        updateTeamRequest.setServiceId(2L); // Change service

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team)); // Original team
        when(serviceService.getServiceEntityById(2L)).thenReturn(newService); // New service
        when(teamRepository.existsByNameAndService(updateTeamRequest.getName(), newService)).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamDto result = teamService.updateTeam(1L, updateTeamRequest);

        assertNotNull(result);
        assertEquals(updateTeamRequest.getName(), result.getName());
        assertEquals(newService.getId(), result.getServiceId());
        assertEquals(newService.getName(), result.getServiceName());
        verify(teamRepository).save(any(Team.class));

    }


    @Test
    void updateTeam_teamNotFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(1L, updateTeamRequest));
    }

    @Test
    void updateTeam_newServiceIdNotFound_throwsResourceNotFoundException() {
        updateTeamRequest.setServiceId(2L); // Try to update to a non-existent service
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(serviceService.getServiceEntityById(2L)).thenThrow(new ResourceNotFoundException("Service not found"));

        assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(1L, updateTeamRequest));
    }

    @Test
    void updateTeam_nameConflict_throwsDuplicateResourceException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByNameAndService(updateTeamRequest.getName(), service)).thenReturn(true);
        // Assuming serviceId in updateTeamRequest is the same as original team's service for this test

        assertThrows(DuplicateResourceException.class, () -> teamService.updateTeam(1L, updateTeamRequest));
    }

    @Test
    void deleteTeam_success() {
        team.getUsers().add(user1); // Team has one user
        user1.getTeams().add(team); // User is part of the team

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.saveAll(anyIterable())).thenReturn(null); // For saving users after removing team
        when(teamRepository.save(any(Team.class))).thenReturn(team); // For saving team after clearing users
        doNothing().when(teamRepository).delete(team);

        assertDoesNotThrow(() -> teamService.deleteTeam(1L));

        verify(userRepository).saveAll(argThat(users -> ((Set<User>)users).contains(user1) && ((Set<User>)users).size()==1));
        assertTrue(user1.getTeams().isEmpty(), "User's team set should be empty after team deletion");
        assertTrue(team.getUsers().isEmpty(), "Team's user set should be cleared before deletion");
        verify(teamRepository).save(team); // verify team is saved after clearing users
        verify(teamRepository).delete(team);
    }


    @Test
    void deleteTeam_notFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.deleteTeam(1L));
    }

    @Test
    void assignUsersToTeam_success() {
        Set<Long> userIds = Set.of(user1.getId(), user2.getId());
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(userRepository.saveAll(anyIterable())).thenReturn(List.of(user1, user2));
        when(teamRepository.save(any(Team.class))).thenReturn(team);


        TeamDto result = teamService.assignUsersToTeam(1L, userIds);

        assertNotNull(result);
        assertEquals(2, team.getUsers().size());
        assertTrue(team.getUsers().contains(user1));
        assertTrue(team.getUsers().contains(user2));
        assertTrue(user1.getTeams().contains(team));
        assertTrue(user2.getTeams().contains(team));
        verify(userRepository).saveAll(anyIterable());
        verify(teamRepository).save(team);
    }

    @Test
    void assignUsersToTeam_teamNotFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.assignUsersToTeam(1L, Set.of(1L)));
    }

    @Test
    void assignUsersToTeam_userNotFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.empty()); // User 1 not found

        assertThrows(ResourceNotFoundException.class, () -> teamService.assignUsersToTeam(1L, Set.of(1L)));
    }

    @Test
    void removeUserFromTeam_success() {
        team.getUsers().add(user1);
        user1.getTeams().add(team);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);
        when(teamRepository.save(any(Team.class))).thenReturn(team);


        TeamDto result = teamService.removeUserFromTeam(1L, 1L);

        assertNotNull(result);
        assertFalse(team.getUsers().contains(user1));
        assertFalse(user1.getTeams().contains(team));
        verify(userRepository).save(user1);
        verify(teamRepository).save(team);
    }

    @Test
    void removeUserFromTeam_teamNotFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.removeUserFromTeam(1L, 1L));
    }

    @Test
    void removeUserFromTeam_userNotFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.removeUserFromTeam(1L, 1L));
    }

    @Test
    void removeUserFromTeam_userNotInTeam_throwsResourceNotFoundException() {
        // User1 is not in the team initially
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        assertThrows(ResourceNotFoundException.class, () -> teamService.removeUserFromTeam(1L, 1L));
    }

    @Test
    void getUsersInTeam_success() {
        team.getUsers().add(user1);
        team.getUsers().add(user2);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        Set<UserDto> result = teamService.getUsersInTeam(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        Set<String> usernames = result.stream().map(UserDto::getUsername).collect(Collectors.toSet());
        assertTrue(usernames.contains("user1"));
        assertTrue(usernames.contains("user2"));
    }

    @Test
    void getUsersInTeam_teamNotFound_throwsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teamService.getUsersInTeam(1L));
    }
}
