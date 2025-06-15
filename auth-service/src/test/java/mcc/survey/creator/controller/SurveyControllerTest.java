package mcc.survey.creator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// JsonNodeFactory and ObjectNode are no longer needed for request DTO
import mcc.survey.creator.dto.SurveyDTO;
import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SurveyControllerTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SurveyController surveyController;

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

        // surveyJson is now a String directly in SurveyDTO
        String surveyJsonString = "{\"question\":\"What is your name?\"}";
        dto.setSurveyJson(objectMapper.readTree(surveyJsonString));

        String username = "testuser";
        User owner = new User();
        owner.setId(1L);
        owner.setUsername(username);

        Survey expectedSavedSurvey = new Survey();
        expectedSavedSurvey.setId(1L);
        expectedSavedSurvey.setTitle(dto.getTitle());
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
    void testCreateSurvey_UserNotFound() throws JsonMappingException, JsonProcessingException {
        // Arrange
        SurveyDTO dto = new SurveyDTO(); // Changed from SurveyCreationRequestDTO to SurveyDTO
        dto.setTitle("Test Survey - User Not Found");
        // surveyJson is now a String directly in SurveyDTO
        dto.setSurveyJson(this.objectMapper.readTree("{\"question\":\"Any question?\"}"));

        String username = "nonexistentuser";
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            surveyController.createSurvey(dto, authentication);
        });

        assertEquals("User not found for token: " + username, exception.getMessage()); // Updated assertion for more specific message
        verify(surveyRepository, never()).save(any(Survey.class));
    }

    // The testCreateSurvey_JsonProcessingException is removed as the controller no longer performs
    // the JsonNode to String conversion for surveyJson that could throw this specific exception.
    // The surveyJson is now accepted as a String directly from SurveyDTO.
}
