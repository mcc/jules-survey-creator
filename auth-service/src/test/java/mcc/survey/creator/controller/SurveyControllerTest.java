package mcc.survey.creator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// JsonNodeFactory and ObjectNode are no longer needed for request DTO
import mcc.survey.creator.dto.SurveyDTO;
import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
// import mcc.survey.creator.service.SurveyService; // Keep commented out as per controller workaround
import mcc.survey.creator.exception.ResourceNotFoundException; // Ensure this exists

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SurveyControllerTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private UserRepository userRepository;

    // @Mock
    // private SurveyService surveyService; // Keep commented as per controller workaround

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SurveyController surveyController;

    private final ObjectMapper objectMapper = new ObjectMapper(); // Used for creating JsonNode and converting

    @BeforeEach
    void setUp() {
        // surveyController = new SurveyController(surveyRepository, userRepository); // If using constructor injection
        // For field injection with @InjectMocks, MockitoAnnotations.openMocks(this) is alternative if not using @ExtendWith
    }

    private ObjectNode createSampleJsonNode() {
        ObjectNode surveyJsonNode = JsonNodeFactory.instance.objectNode();
        surveyJsonNode.put("question", "What is your favorite color?");
        surveyJsonNode.put("type", "text");
        return surveyJsonNode;
    }

    private SurveyDTO createSurveyDTO(Long id, String title, JsonNode surveyJson) {
        SurveyDTO dto = new SurveyDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setSurveyJson(surveyJson);
        dto.setOwner(new mcc.survey.creator.dto.UserDTO(1L, "testuser"));
        dto.setCreatedAt(Timestamp.from(Instant.now()));
        dto.setUpdatedAt(Timestamp.from(Instant.now()));
        return dto;
    }
     private Survey createSurveyEntity(Long id, String title, String surveyJsonString, User owner) {
        Survey survey = new Survey();
        survey.setId(id);
        survey.setTitle(title);
        survey.setSurveyJson(surveyJsonString); // String form
        survey.setOwner(owner);
        survey.setCreatedAt(Timestamp.from(Instant.now()));
        survey.setUpdatedAt(Timestamp.from(Instant.now()));
        return survey;
    }

    private final ObjectMapper objectMapper = new ObjectMapper(); // Keep for potential response validation or other tests

    @Test
    void testCreateSurvey_Success() throws Exception {
        // Arrange
        SurveyDTO dto = new SurveyDTO(); // Changed from SurveyCreationRequestDTO to SurveyDTO
        dto.setTitle("Test Survey");
        dto.setDescription("Test Description");
        dto.setSurveyMode("Test Mode");
        dto.setDataClassification("Test Classification");
        dto.setStatus("Test Status");
        ObjectNode surveyJsonNode = createSampleJsonNode();
        dto.setSurveyJson(surveyJsonNode);
        // surveyJson is now a String directly in SurveyDTO
        String surveyJsonString = "{\"question\":\"What is your name?\"}";
        dto.setSurveyJson(surveyJsonString);

        String username = "testuser";
        User owner = new User();
        owner.setId(1L);
        owner.setUsername(username);

        Survey expectedSavedSurvey = createSurveyEntity(1L, dto.getTitle(), objectMapper.writeValueAsString(surveyJsonNode), owner);
        // Populate other fields from DTO for expectedSavedSurvey
        expectedSavedSurvey.setDescription(dto.getDescription());
        expectedSavedSurvey.setSurveyJson(surveyJsonString); // Use the string directly
        expectedSavedSurvey.setOwner(owner);

        expectedSavedSurvey.setSurveyMode(dto.getSurveyMode());
        expectedSavedSurvey.setDataClassification(dto.getDataClassification());
        expectedSavedSurvey.setStatus(dto.getStatus());


        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner));
        when(surveyRepository.save(any(Survey.class))).thenReturn(expectedSavedSurvey);

        // Act
        ResponseEntity<SurveyDTO> response = surveyController.createSurvey(dto, authentication);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        SurveyDTO responseBody = response.getBody();
        assertEquals(expectedSavedSurvey.getTitle(), responseBody.getTitle());
        assertEquals(expectedSavedSurvey.getDescription(), responseBody.getDescription());

        // Assert surveyJson is JsonNode and content matches
        assertNotNull(responseBody.getSurveyJson());
        assertTrue(responseBody.getSurveyJson() instanceof com.fasterxml.jackson.databind.JsonNode, "SurveyJson should be a JsonNode");
        assertEquals(surveyJsonNode, responseBody.getSurveyJson(), "SurveyJson content should match");

        assertEquals(expectedSavedSurvey.getOwner().getUsername(), responseBody.getOwner().getUsername());
        assertEquals(expectedSavedSurvey.getTitle(), response.getBody().getTitle());
        assertEquals(expectedSavedSurvey.getDescription(), response.getBody().getDescription());
        assertEquals(expectedSavedSurvey.getSurveyJson(), response.getBody().getSurveyJson());
        assertEquals(expectedSavedSurvey.getOwner().getUsername(), response.getBody().getOwner().getUsername());


        ArgumentCaptor<Survey> surveyArgumentCaptor = ArgumentCaptor.forClass(Survey.class);
        verify(surveyRepository).save(surveyArgumentCaptor.capture());
        Survey capturedSurvey = surveyArgumentCaptor.getValue();

        assertEquals(dto.getTitle(), capturedSurvey.getTitle());
        assertEquals(dto.getSurveyJson(), capturedSurvey.getSurveyJson()); // surveyJson is already a string
        assertEquals(owner, capturedSurvey.getOwner());
    }

    @Test
    void testCreateSurvey_UserNotFound() {
        // Arrange
        SurveyDTO dto = new SurveyDTO(); // Changed from SurveyCreationRequestDTO to SurveyDTO
        dto.setTitle("Test Survey - User Not Found");
        dto.setSurveyJson(createSampleJsonNode());

        String username = "nonexistentuser";
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            surveyController.createSurvey(dto, authentication);
        });

        assertEquals("User not found for token: " + username, exception.getMessage()); // Adjusted to match actual message
        verify(surveyRepository, never()).save(any(Survey.class));
    }

    @Test
    void testGetSurveyById_Success() throws Exception {
        // Arrange
        Long surveyId = 1L;
        String username = "testuser";
        User owner = new User();
        owner.setId(1L);
        owner.setUsername(username);

        ObjectNode surveyJsonNode = createSampleJsonNode();
        String surveyJsonString = objectMapper.writeValueAsString(surveyJsonNode);
        Survey mockSurvey = createSurveyEntity(surveyId, "Sample Survey", surveyJsonString, owner);

        when(authentication.getName()).thenReturn(username); // For logging, @PreAuthorize might need more
        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(mockSurvey));
        // Assuming @PreAuthorize conditions are met or SurveySecurityService is mocked elsewhere if applicable

        // Act
        ResponseEntity<SurveyDTO> response = surveyController.getSurveyById(surveyId, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        SurveyDTO responseBody = response.getBody();
        assertEquals(mockSurvey.getTitle(), responseBody.getTitle());

        assertNotNull(responseBody.getSurveyJson());
        assertTrue(responseBody.getSurveyJson() instanceof com.fasterxml.jackson.databind.JsonNode, "SurveyJson should be a JsonNode");
        assertEquals(surveyJsonNode, responseBody.getSurveyJson(), "SurveyJson content should match the original data");
    }

    @Test
    void testGetSurveyById_NotFound() {
        // Arrange
        Long surveyId = 2L;
        when(authentication.getName()).thenReturn("testuser");
        when(surveyRepository.findById(surveyId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<SurveyDTO> response = surveyController.getSurveyById(surveyId, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetSurveyById_JsonParseException() {
        // Arrange
        Long surveyId = 3L;
        String username = "testuser";
        User owner = new User();
        owner.setId(1L);
        owner.setUsername(username);

        String invalidJsonString = "{\"question\": \"What is your name?, \"type\": \"text\""; // Missing closing quote
        Survey mockSurveyWithInvalidJson = createSurveyEntity(surveyId, "Invalid JSON Survey", invalidJsonString, owner);

        when(authentication.getName()).thenReturn(username);
        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(mockSurveyWithInvalidJson));

        // Act
        ResponseEntity<SurveyDTO> response = surveyController.getSurveyById(surveyId, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Controller currently returns OK with potentially null/malformed DTO parts
        assertNotNull(response.getBody());
        // The controller logs an error and continues, so surveyJson in DTO might be null
        assertNull(response.getBody().getSurveyJson(), "SurveyJson should be null due to parsing error");
    }

    @Test
    void testUpdateSurvey_Success() throws Exception {
        // Arrange
        Long surveyId = 1L;
        String username = "testuser";
        User owner = new User();
        owner.setId(1L);
        owner.setUsername(username);

        ObjectNode originalJsonNode = objectMapper.createObjectNode().put("oldField", "oldValue");
        String originalJsonString = objectMapper.writeValueAsString(originalJsonNode);
        Survey existingSurvey = createSurveyEntity(surveyId, "Old Title", originalJsonString, owner);

        ObjectNode updatedJsonNode = createSampleJsonNode(); // New JSON content for update
        SurveyDTO surveyDetailsDTO = createSurveyDTO(surveyId, "New Updated Title", updatedJsonNode);
        surveyDetailsDTO.setDescription("New Description");
        surveyDetailsDTO.setSurveyMode("Updated Mode");
        surveyDetailsDTO.setDataClassification("Updated Classification");
        surveyDetailsDTO.setStatus("Updated Status");

        Survey surveyToReturnFromSave = new Survey(); // What surveyRepository.save will return
        surveyToReturnFromSave.setId(surveyId);
        surveyToReturnFromSave.setTitle(surveyDetailsDTO.getTitle());
        surveyToReturnFromSave.setDescription(surveyDetailsDTO.getDescription());
        surveyToReturnFromSave.setSurveyJson(objectMapper.writeValueAsString(updatedJsonNode)); // String form
        surveyToReturnFromSave.setOwner(owner);
        surveyToReturnFromSave.setSurveyMode(surveyDetailsDTO.getSurveyMode());
        surveyToReturnFromSave.setDataClassification(surveyDetailsDTO.getDataClassification());
        surveyToReturnFromSave.setStatus(surveyDetailsDTO.getStatus());


        when(authentication.getName()).thenReturn(username);
        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(existingSurvey));
        // This simulates the @PreAuthorize check for ownership which is not the focus here.
        // We assume SurveySecurityService would allow this if it were active.

        // ArgumentCaptor for the Survey entity passed to save
        ArgumentCaptor<Survey> surveyArgumentCaptor = ArgumentCaptor.forClass(Survey.class);
        when(surveyRepository.save(surveyArgumentCaptor.capture())).thenReturn(surveyToReturnFromSave);


        // Act
        ResponseEntity<SurveyDTO> response = surveyController.updateSurvey(surveyId, surveyDetailsDTO, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        SurveyDTO responseBody = response.getBody();

        // Verify the Survey entity captured by surveyRepository.save()
        Survey capturedSurvey = surveyArgumentCaptor.getValue();
        assertEquals(surveyDetailsDTO.getTitle(), capturedSurvey.getTitle());
        assertEquals(surveyDetailsDTO.getDescription(), capturedSurvey.getDescription());
        // Assert that the JsonNode from DTO was correctly stringified for the entity
        assertEquals(objectMapper.writeValueAsString(updatedJsonNode), capturedSurvey.getSurveyJson());
        assertEquals(surveyDetailsDTO.getSurveyMode(), capturedSurvey.getSurveyMode());

        // Assert the response DTO
        assertEquals(surveyDetailsDTO.getTitle(), responseBody.getTitle());
        assertNotNull(responseBody.getSurveyJson());
        assertTrue(responseBody.getSurveyJson() instanceof com.fasterxml.jackson.databind.JsonNode, "Response SurveyJson should be JsonNode");
        assertEquals(updatedJsonNode, responseBody.getSurveyJson(), "Response SurveyJson content should match updated content");
    }

    @Test
    void testUpdateSurvey_NotFound() {
        // Arrange
        Long surveyId = 99L; // Non-existent survey
        String username = "testuser";
        SurveyDTO surveyDetailsDTO = createSurveyDTO(surveyId, "Update Title", createSampleJsonNode());

        when(authentication.getName()).thenReturn(username);
        when(surveyRepository.findById(surveyId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            surveyController.updateSurvey(surveyId, surveyDetailsDTO, authentication);
        });
        assertEquals("Survey not found with id " + surveyId, exception.getMessage());
        verify(surveyRepository, never()).save(any(Survey.class));
    }
}
