package mcc.survey.creator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mcc.survey.creator.dto.SurveyCreationRequestDTO;
import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.service.SurveyService;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {
    Logger logger = org.slf4j.LoggerFactory.getLogger(SurveyController.class);

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/createSurvey")
    @PreAuthorize("hasAuthority('OP_CREATE_SURVEY')")
    public ResponseEntity<Survey> createSurvey(@RequestBody SurveyCreationRequestDTO surveyDTO, Authentication authentication) {
        Survey survey = new Survey();
        survey.setTitle(surveyDTO.getTitle() != null ? surveyDTO.getTitle() : "Untitled Survey");

        survey.setDescription(surveyDTO.getDescription()!= null ? surveyDTO.getDescription() : "No description provided");

        survey.setSurveyMode(surveyDTO.getSurveyMode());
        survey.setDataClassification(surveyDTO.getDataClassification());
        survey.setStatus(surveyDTO.getStatus());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            survey.setSurveyJson(objectMapper.writeValueAsString(surveyDTO.getSurveyJson()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Let GlobalExceptionHandler handle this as a generic Exception or a specific one if defined
            throw new RuntimeException("Error processing survey JSON: " + e.getMessage(), e);
        }

        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for token"));
        survey.setOwner(user);
        logger.info(user.getUsername());
        Survey savedSurvey = surveyRepository.save(survey);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSurvey);
    }

    @GetMapping("/")
    @PreAuthorize("hasAuthority('OP_VIEW_OWN_SURVEY')")
    public ResponseEntity<List<Survey>> getAllSurveysForUser(Authentication authentication) {
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for token"));
        List<Survey> surveys = surveyRepository.findByOwnerId(user.getId());
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/{surveyId}")
    @PreAuthorize("hasAuthority('OP_VIEW_OWN_SURVEY') and hasAuthority('OP_VIEW_ALL_SURVEYS')")
    public ResponseEntity<Survey> getSurveyById(@PathVariable Long surveyId) {
        String userId = getCurrentUserId();
        Optional<Survey> surveyOptional = surveyService.getSurveyByIdAndUsername(surveyId, userId);
        logger.info("Fetching survey with ID: " + surveyId + " for user: " + userId);
        if (surveyOptional.isEmpty()) {
            logger.warn("Survey with ID: " + surveyId + " not found for user: " + userId);
        } else {
            logger.info("Found survey: " + surveyOptional.get().getTitle() + " for user: " + userId);
        }

        return surveyOptional
                .map(survey -> new ResponseEntity<>(survey, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with ID: " + surveyId + " for user: " + userId));
    }

    @PostMapping("/{id}/share/{userId}")
    @PreAuthorize("hasAuthority('OP_SHARE_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Survey> shareSurvey(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        Optional<User> userToShareWithOptional = userRepository.findById(userId);

        if (surveyOptional.isPresent() && userToShareWithOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            User userToShareWith = userToShareWithOptional.get();

            User owner = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Owner not found for token"));

            // Prevent sharing with oneself
            if (owner.getId().equals(userId)) {
                throw new IllegalArgumentException("Cannot share survey with oneself.");
            }

            survey.getSharedWithUsers().add(userToShareWith);
            surveyRepository.save(survey);
            return ResponseEntity.ok(survey);
        } else {
            throw new ResourceNotFoundException("Survey or User to share with not found.");
        }
    }

    @DeleteMapping("/{id}/unshare/{userId}")
    @PreAuthorize("hasAuthority('OP_SHARE_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Survey> unshareSurvey(@PathVariable Long id, @PathVariable Long userId) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        Optional<User> userToUnshareOptional = userRepository.findById(userId);

        if (surveyOptional.isPresent() && userToUnshareOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            User userToUnshare = userToUnshareOptional.get();

            // Ownership is already checked by @PreAuthorize
            survey.getSharedWithUsers().remove(userToUnshare);
            surveyRepository.save(survey);
            return ResponseEntity.ok(survey);
        } else {
            throw new ResourceNotFoundException("Survey or User to unshare not found.");
        }
    }


    @PutMapping("/{surveyId}")
    @PreAuthorize("hasAuthority('OP_EDIT_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Survey> updateSurvey(@PathVariable Long surveyId, @RequestBody Survey surveyDetails) {
        String userId = getCurrentUserId();
        // ResourceNotFoundException will be thrown by the service if not found / not owned
        Survey updatedSurvey = surveyService.updateSurvey(surveyId, surveyDetails, userId);
        return new ResponseEntity<>(updatedSurvey, HttpStatus.OK);
    }

    @DeleteMapping("/{surveyId}")
    @PreAuthorize("hasAuthority('OP_DELETE_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long surveyId) {
        String userId = getCurrentUserId();
        // ResourceNotFoundException will be thrown by the service if not found / not owned
        surveyService.deleteSurvey(surveyId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    private final SurveyService surveyService;
    // private final UserService userService; // Inject if using userService.getCurrentUserId()

    @Autowired
    public SurveyController(SurveyService surveyService /*, UserService userService */) {
        this.surveyService = surveyService;
        // this.userService = userService;
    }

    // Placeholder for getting current user ID. Replace with Spring Security context later.
    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
             return ((UserDetails) principal).getUsername(); // Or a custom User object with an ID
        } else if (principal instanceof String) {
             return (String) principal;
        }
        return "anonymousUser"; // Or throw an exception if user must be authenticated
        
    }

    @GetMapping
    public List<Survey> getSurveysForCurrentUser() {
        String username = getCurrentUserId();
        return surveyService.getSurveysByUsername(username);
    }

}
