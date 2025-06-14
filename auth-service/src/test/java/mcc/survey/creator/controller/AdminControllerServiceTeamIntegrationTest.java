package mcc.survey.creator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mcc.survey.creator.dto.*;
import mcc.survey.creator.model.Service;
import mcc.survey.creator.model.Team;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.ServiceRepository;
import mcc.survey.creator.repository.TeamRepository;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.service.ServiceService;
import mcc.survey.creator.service.TeamService;
import mcc.survey.creator.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Ensure a test profile is used, potentially with an H2 database
@Transactional // Rollback transactions after each test
class AdminControllerServiceTeamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService; // For creating test users

    // To help create entities for testing updates/deletes
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private TeamService teamService;


    private ServiceDto testServiceDto;
    private TeamDto testTeamDto;
    private User testUserSystemAdmin;
    private User testUserRegular;


    @BeforeEach
    void setUp() {
        // Clean up before each test to ensure isolation
        userRepository.deleteAll();
        teamRepository.deleteAll();
        serviceRepository.deleteAll();


        // Create a user with ROLE_SYSTEM_ADMIN for service/team creation in setup
        CreateUserRequest systemAdminRequest = new CreateUserRequest();
        systemAdminRequest.setUsername("test_sysadmin");
        systemAdminRequest.setEmail("sysadmin@example.com");
        systemAdminRequest.setPassword("password"); // UserService will encode this
        systemAdminRequest.setRoles(Set.of("ROLE_SYSTEM_ADMIN"));
        // userService.createUser will return UserDto, but we might need User entity for other setups
        // For simplicity, we'll rely on @WithMockUser for most auth, but a real admin might be needed for setup
        UserDto systemAdminUserDto = userService.createUser(systemAdminRequest);
        testUserSystemAdmin = userService.getUserEntityById(systemAdminUserDto.getId());


        CreateUserRequest regularUserRequest = new CreateUserRequest();
        regularUserRequest.setUsername("test_regularuser");
        regularUserRequest.setEmail("regular@example.com");
        regularUserRequest.setPassword("password");
        regularUserRequest.setRoles(Set.of("ROLE_USER"));
        UserDto regularUserDto = userService.createUser(regularUserRequest);
        testUserRegular = userService.getUserEntityById(regularUserDto.getId());
    }

    // Helper method to create a service for tests that need an existing service
    private ServiceDto createAService(String name) throws Exception {
        CreateServiceRequest createService = new CreateServiceRequest();
        createService.setName(name);
        createService.setDescription(name + " description");

        String responseString = mockMvc.perform(post("/api/admin/services")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test_sysadmin").roles("SYSTEM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createService)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseString, ServiceDto.class);
    }

    // Helper method to create a team for tests that need an existing team
    private TeamDto createATeam(String name, Long serviceId) throws Exception {
        CreateTeamRequest createTeam = new CreateTeamRequest();
        createTeam.setName(name);
        createTeam.setDescription(name + " description");
        createTeam.setServiceId(serviceId);

        String responseString = mockMvc.perform(post("/api/admin/teams")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test_sysadmin").roles("SYSTEM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeam)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseString, TeamDto.class);
    }


    // --- Service Endpoint Tests ---

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void createService_asSystemAdmin_success() throws Exception {
        CreateServiceRequest createServiceRequest = new CreateServiceRequest();
        createServiceRequest.setName("New Service");
        createServiceRequest.setDescription("New Service Description");

        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createServiceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Service")))
                .andExpect(jsonPath("$.description", is("New Service Description")));
    }

    @Test
    @WithMockUser(username = "test_useradmin", roles = {"USER_ADMIN"})
    void createService_asUserAdmin_forbidden() throws Exception {
        CreateServiceRequest createServiceRequest = new CreateServiceRequest();
        createServiceRequest.setName("Forbidden Service");
        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createServiceRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void createService_blankName_badRequest() throws Exception {
        CreateServiceRequest createServiceRequest = new CreateServiceRequest();
        createServiceRequest.setName(""); // Blank name
        createServiceRequest.setDescription("Description for blank name service");

        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createServiceRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void createService_duplicateName_conflict() throws Exception {
        createAService("Unique Service Name"); // Create it first

        CreateServiceRequest duplicateServiceRequest = new CreateServiceRequest();
        duplicateServiceRequest.setName("Unique Service Name");
        duplicateServiceRequest.setDescription("Description for duplicate service");

        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateServiceRequest)))
                .andExpect(status().isConflict()); // Expecting 409 Conflict
    }


    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getAllServices_asSystemAdmin_success() throws Exception {
        createAService("Service 1");
        createAService("Service 2");

        mockMvc.perform(get("/api/admin/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Service 1")))
                .andExpect(jsonPath("$[1].name", is("Service 2")));
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getServiceById_asSystemAdmin_success() throws Exception {
        ServiceDto createdService = createAService("My Service");

        mockMvc.perform(get("/api/admin/services/" + createdService.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdService.getId().intValue())))
                .andExpect(jsonPath("$.name", is("My Service")));
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getServiceById_asSystemAdmin_notFound() throws Exception {
        mockMvc.perform(get("/api/admin/services/9999")) // Non-existent ID
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void updateService_asSystemAdmin_success() throws Exception {
        ServiceDto createdService = createAService("Original Service");
        UpdateServiceRequest updateRequest = new UpdateServiceRequest();
        updateRequest.setName("Updated Service Name");
        updateRequest.setDescription("Updated service description");

        mockMvc.perform(put("/api/admin/services/" + createdService.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Service Name")))
                .andExpect(jsonPath("$.description", is("Updated service description")));
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void updateService_asSystemAdmin_notFound() throws Exception {
        UpdateServiceRequest updateRequest = new UpdateServiceRequest();
        updateRequest.setName("Updated Service Name");
        mockMvc.perform(put("/api/admin/services/9999") // Non-existent ID
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void deleteService_asSystemAdmin_success() throws Exception {
        ServiceDto createdService = createAService("Service To Delete");
        mockMvc.perform(delete("/api/admin/services/" + createdService.getId()))
                .andExpect(status().isNoContent()); // Or .isOk() depending on controller impl.
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void deleteService_asSystemAdmin_hasAssociatedTeams_conflict() throws Exception {
        ServiceDto serviceWithTeam = createAService("Service With Team");
        createATeam("Team In Service", serviceWithTeam.getId());

        mockMvc.perform(delete("/api/admin/services/" + serviceWithTeam.getId()))
                .andExpect(status().isConflict()); // Or BadRequest, check AdminController
    }


    // --- Team Endpoint Tests ---
    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void createTeam_asSystemAdmin_success() throws Exception {
        ServiceDto serviceForTeam = createAService("ServiceForTeam");
        CreateTeamRequest createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setName("New Team");
        createTeamRequest.setServiceId(serviceForTeam.getId());

        mockMvc.perform(post("/api/admin/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeamRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Team")))
                .andExpect(jsonPath("$.serviceId", is(serviceForTeam.getId().intValue())));
    }

    @Test
    @WithMockUser(username = "test_useradmin", roles = {"USER_ADMIN"})
    void createTeam_asUserAdmin_forbidden() throws Exception {
        ServiceDto serviceForTeam = createAService("ServiceForTeamForbidden"); // Needs sysadmin to create service
        CreateTeamRequest createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setName("Forbidden Team");
        createTeamRequest.setServiceId(serviceForTeam.getId()); // serviceId needs to be valid

        mockMvc.perform(post("/api/admin/teams")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test_useradmin").roles("USER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeamRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void createTeam_nonExistentServiceId_badRequestOrNotFound() throws Exception {
        CreateTeamRequest createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setName("Team with Invalid Service");
        createTeamRequest.setServiceId(9999L); // Non-existent service ID

        mockMvc.perform(post("/api/admin/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeamRequest)))
                .andExpect(status().isBadRequest()); // Or isNotFound based on AdminController advice
    }


    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getAllTeams_asSystemAdmin_success() throws Exception {
        ServiceDto service1 = createAService("Service A");
        createATeam("Team Alpha", service1.getId());
        ServiceDto service2 = createAService("Service B");
        createATeam("Team Beta", service2.getId());


        mockMvc.perform(get("/api/admin/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Team Alpha")))
                .andExpect(jsonPath("$[1].name", is("Team Beta")));
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getTeamsByService_asSystemAdmin_success() throws Exception {
        ServiceDto service = createAService("Target Service");
        createATeam("Team 1 in Target", service.getId());
        createATeam("Team 2 in Target", service.getId());
        ServiceDto otherService = createAService("Other Service"); // Create another service
        createATeam("Team in Other", otherService.getId()); // And a team in it

        mockMvc.perform(get("/api/admin/services/" + service.getId() + "/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].serviceId", is(service.getId().intValue())))
                .andExpect(jsonPath("$[1].serviceId", is(service.getId().intValue())));
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getTeamsByService_asSystemAdmin_serviceNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/services/8888/teams")) // Non-existent service ID
                .andExpect(status().isNotFound());
    }


    // --- Team Membership Endpoint Tests ---
    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void assignUsersToTeam_asSystemAdmin_success() throws Exception {
        ServiceDto service = createAService("Membership Service");
        TeamDto team = createATeam("Membership Team", service.getId());

        // We have testUserRegular created in @BeforeEach
        UserTeamAssignmentRequest assignmentRequest = new UserTeamAssignmentRequest();
        assignmentRequest.setUserIds(Set.of(testUserRegular.getId()));

        mockMvc.perform(post("/api/admin/teams/" + team.getId() + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentRequest)))
                .andExpect(status().isOk())
                // Check if team DTO in response contains user (TeamService doesn't add users to DTO)
                // Instead, verify by fetching users in team
                .andDo(result -> { // Check after assignment
                    mockMvc.perform(get("/api/admin/teams/" + team.getId() + "/users")
                                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test_sysadmin").roles("SYSTEM_ADMIN")))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", hasSize(1)))
                            .andExpect(jsonPath("$[0].id", is(testUserRegular.getId().intValue())));
                });
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void assignUsersToTeam_asSystemAdmin_teamNotFound() throws Exception {
        UserTeamAssignmentRequest assignmentRequest = new UserTeamAssignmentRequest();
        assignmentRequest.setUserIds(Set.of(testUserRegular.getId()));

        mockMvc.perform(post("/api/admin/teams/7777/users") // Non-existent team ID
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void assignUsersToTeam_asSystemAdmin_userNotFoundInPayload() throws Exception {
        ServiceDto service = createAService("Membership Service User Not Found");
        TeamDto team = createATeam("Membership Team User Not Found", service.getId());

        UserTeamAssignmentRequest assignmentRequest = new UserTeamAssignmentRequest();
        assignmentRequest.setUserIds(Set.of(6666L)); // Non-existent user ID

        mockMvc.perform(post("/api/admin/teams/" + team.getId() + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentRequest)))
                .andExpect(status().isNotFound()); // Because UserService throws ResourceNotFound for user
    }


    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void removeUserFromTeam_asSystemAdmin_success() throws Exception {
        ServiceDto service = createAService("Removal Service");
        TeamDto team = createATeam("Removal Team", service.getId());

        // Assign user first
        UserTeamAssignmentRequest assignReq = new UserTeamAssignmentRequest();
        assignReq.setUserIds(Set.of(testUserRegular.getId()));
        mockMvc.perform(post("/api/admin/teams/" + team.getId() + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // Then remove
        mockMvc.perform(delete("/api/admin/teams/" + team.getId() + "/users/" + testUserRegular.getId()))
                .andExpect(status().isOk());

         // Verify by fetching users in team
        mockMvc.perform(get("/api/admin/teams/" + team.getId() + "/users")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test_sysadmin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "test_sysadmin", roles = {"SYSTEM_ADMIN"})
    void getUsersInTeam_asSystemAdmin_success() throws Exception {
        ServiceDto service = createAService("GetUsers Service");
        TeamDto team = createATeam("GetUsers Team", service.getId());

        UserTeamAssignmentRequest assignReq = new UserTeamAssignmentRequest();
        assignReq.setUserIds(Set.of(testUserRegular.getId()));
         mockMvc.perform(post("/api/admin/teams/" + team.getId() + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/teams/" + team.getId() + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is(testUserRegular.getUsername())));
    }
}
