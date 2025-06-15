package mcc.survey.creator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mcc.survey.creator.dto.SurveyCreationRequestDTO;
import mcc.survey.creator.dto.SurveyDTO; // Added
import mcc.survey.creator.dto.UserDTO;   // Added
import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.service.SurveyService;
import mcc.survey.creator.exception.ResourceNotFoundException; // Ensure this exists

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Added
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; // Keep if getCurrentUserId() is kept for some reason
import org.springframework.security.core.userdetails.UserDetails;      // Keep if getCurrentUserId() is kept
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid; // Added for @Valid on DTO

import java.util.List; // Added
import java.util.Optional;
import java.util.Set;   // Added
import java.util.stream.Collectors; // Added

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {
    private static final Logger logger = LoggerFactory.getLogger(SurveyController.class);

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SurveyService surveyService;
    // private final UserService userService; // Inject if using userService.getCurrentUserId()

    // Removed constructor SurveyController(SurveyService surveyService) to use field injection,
    // or it can be added back if constructor injection is preferred for all autowired fields.
    // For now, assuming field injection for surveyService as well, similar to repositories.


    private UserDTO convertToUserDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(user.getId(), user.getUsername());
    }

    private SurveyDTO convertToSurveyDTO(Survey survey) {
        if (survey == null) {
            return null;
        }
        SurveyDTO dto = new SurveyDTO();
        dto.setId(survey.getId());
        dto.setTitle(survey.getTitle());
        dto.setDescription(survey.getDescription());
        dto.setSurveyJson(survey.getSurveyJson()); // This holds the SurveyJS JSON definition
        dto.setCreatedAt(survey.getCreatedAt());
        dto.setUpdatedAt(survey.getUpdatedAt());
        dto.setSurveyMode(survey.getSurveyMode());
        dto.setDataClassification(survey.getDataClassification());
        dto.setStatus(survey.getStatus());
        if (survey.getOwner() != null) {
            dto.setOwner(convertToUserDTO(survey.getOwner()));
        }
        if (survey.getSharedWithUsers() != null) {
            dto.setSharedWithUsers(survey.getSharedWithUsers().stream()
                                       .map(this::convertToUserDTO)
                                       .collect(Collectors.toSet()));
        }
        return dto;
    }

    @PostMapping("/createSurvey")
    @PreAuthorize("hasAuthority('OP_CREATE_SURVEY')")
    public ResponseEntity<SurveyDTO> createSurvey(@Valid @RequestBody SurveyDTO surveyDTO, Authentication authentication) {
        Survey survey = new Survey();
        survey.setTitle(surveyDTO.getTitle() != null ? surveyDTO.getTitle() : "Untitled Survey");
        survey.setDescription(surveyDTO.getDescription()!= null ? surveyDTO.getDescription() : "No description provided");
        survey.setSurveyMode(surveyDTO.getSurveyMode());
        survey.setDataClassification(surveyDTO.getDataClassification());
        survey.setStatus(surveyDTO.getStatus());
        // surveyJson is now directly a String in SurveyDTO, so no conversion is needed.
        survey.setSurveyJson(surveyDTO.getSurveyJson());

        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for token: " + currentPrincipalName));
        survey.setOwner(user);
        logger.info("Survey titled '{}' being created by user: {}", survey.getTitle(), user.getUsername());
        Survey savedSurvey = surveyRepository.save(survey);
        SurveyDTO savedSurveyDTO = convertToSurveyDTO(savedSurvey);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSurveyDTO);
    }

    @GetMapping("/")
    @PreAuthorize("hasAuthority('OP_VIEW_OWN_SURVEY')")
    public ResponseEntity<List<SurveyDTO>> getAllSurveysForUser(Authentication authentication) {
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for token: " + currentPrincipalName));
        // This fetches surveys owned by the user.
        List<Survey> surveys = surveyRepository.findByOwnerId(user.getId());
        List<SurveyDTO> surveyDTOs = surveys.stream()
                                           .map(this::convertToSurveyDTO)
                                           .collect(Collectors.toList());
        logger.info("User {} fetched {} surveys via getAllSurveysForUser.", currentPrincipalName, surveyDTOs.size());
        return ResponseEntity.ok(surveyDTOs);
    }

    @GetMapping("/{surveyId}")
    @PreAuthorize("hasAuthority('OP_VIEW_ALL_SURVEYS') or " +
                  "(hasAuthority('OP_VIEW_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #surveyId)) or " +
                  "@surveySecurityService.isSharedWith(authentication, #surveyId)")
    public ResponseEntity<SurveyDTO> getSurveyById(@PathVariable Long surveyId, Authentication authentication) {
        String currentUsername = authentication.getName();
        logger.info("User {} attempting to fetch survey with ID: {}", currentUsername, surveyId);
        Optional<Survey> surveyOptional = surveyRepository.findById(surveyId);

        if (surveyOptional.isEmpty()) {
            logger.warn("Survey with ID: {} not found.", surveyId);
            return ResponseEntity.notFound().build();
        }

        // Access control is handled by @PreAuthorize.
        Survey survey = surveyOptional.get();
        logger.info("User {} accessed survey: '{}' (ID: {}).", currentUsername, survey.getTitle(), surveyId);
        SurveyDTO surveyDTO = convertToSurveyDTO(survey);
        return ResponseEntity.ok(surveyDTO);
    }

    @PostMapping("/{id}/share/{userId}")
    @PreAuthorize("hasAuthority('OP_SHARE_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<SurveyDTO> shareSurvey(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isEmpty()) {
            logger.warn("Share op failed: Survey ID {} not found.", id);
            return ResponseEntity.notFound().build();
        }

        Optional<User> userToShareWithOptional = userRepository.findById(userId);
        if (userToShareWithOptional.isEmpty()) {
            logger.warn("Share op failed: User ID {} to share with not found.", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or an error DTO
        }

        Survey survey = surveyOptional.get();
        User userToShareWith = userToShareWithOptional.get();

        // Check if sharing with self
        if (survey.getOwner().getId().equals(userId)) {
            logger.warn("Attempt to share survey ID {} with its owner (ID {}) - operation denied.", id, userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(convertToSurveyDTO(survey));
        }

        if (survey.getSharedWithUsers().contains(userToShareWith)) {
            logger.info("Survey ID {} is already shared with user ID {}.", id, userId);
        } else {
            survey.getSharedWithUsers().add(userToShareWith);
            surveyRepository.save(survey);
            logger.info("Survey ID {} successfully shared with user ID {}.", id, userId);
        }
        return ResponseEntity.ok(convertToSurveyDTO(survey));
    }

    @DeleteMapping("/{id}/unshare/{userId}")
    @PreAuthorize("hasAuthority('OP_SHARE_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<SurveyDTO> unshareSurvey(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isEmpty()) {
            logger.warn("Unshare op failed: Survey ID {} not found.", id);
            return ResponseEntity.notFound().build();
        }
        Survey survey = surveyOptional.get();

        Optional<User> userToUnshareOptional = userRepository.findById(userId);
        if (userToUnshareOptional.isEmpty()) {
            logger.warn("User ID {} to unshare not found. No change to sharing for survey ID {}.", userId, id);
            return ResponseEntity.ok(convertToSurveyDTO(survey)); // No user to unshare, current state is fine.
        }
        User userToUnshare = userToUnshareOptional.get();

        if (survey.getSharedWithUsers().remove(userToUnshare)) {
            surveyRepository.save(survey);
            logger.info("Survey ID {} successfully unshared from user ID {}.", id, userId);
        } else {
            logger.info("Survey ID {} was not shared with user ID {} or already unshared.", id, userId);
        }
        return ResponseEntity.ok(convertToSurveyDTO(survey));
    }

    @PutMapping("/{surveyId}")
    @PreAuthorize("hasAuthority('OP_EDIT_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #surveyId)")
    public ResponseEntity<SurveyDTO> updateSurvey(@PathVariable Long surveyId,
                                                @Valid @RequestBody SurveyDTO surveyDetailsDTO,
                                                Authentication authentication) {
        String currentUsername = authentication.getName();
        logger.info("User {} attempting to update survey ID {}.", currentUsername, surveyId);

        Survey surveyToUpdate = surveyRepository.findById(surveyId)
            .orElseThrow(() -> {
                logger.warn("Update failed: Survey ID {} not found for user {}.", surveyId, currentUsername);
                return new ResourceNotFoundException("Survey not found with id " + surveyId);
            });

        // Update fields from DTO
        surveyToUpdate.setTitle(surveyDetailsDTO.getTitle());
        surveyToUpdate.setDescription(surveyDetailsDTO.getDescription());
        if (surveyDetailsDTO.getSurveyJson() != null) { // Ensure surveyJson is not accidentally nulled if not provided
            surveyToUpdate.setSurveyJson(surveyDetailsDTO.getSurveyJson());
        }
        surveyToUpdate.setSurveyMode(surveyDetailsDTO.getSurveyMode());
        surveyToUpdate.setDataClassification(surveyDetailsDTO.getDataClassification());
        surveyToUpdate.setStatus(surveyDetailsDTO.getStatus());
        // Owner and sharedWithUsers are not updated via this method.

        // The service method surveyService.updateSurvey(Long surveyId, Survey surveyDetails, String userId)
        // surveyDetails here is the entity with new values.
        Survey updatedSurveyEntity = surveyService.updateSurvey(surveyId, surveyToUpdate, currentUsername);

        SurveyDTO updatedSurveyDTO = convertToSurveyDTO(updatedSurveyEntity);
        logger.info("Survey ID {} updated by user {}. New title: {}", surveyId, currentUsername, updatedSurveyDTO.getTitle());
        return new ResponseEntity<>(updatedSurveyDTO, HttpStatus.OK);
    }

    @DeleteMapping("/{surveyId}")
    @PreAuthorize("hasAuthority('OP_DELETE_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #surveyId)")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long surveyId, Authentication authentication) {
        // String userId = getCurrentUserId(); // Replaced by authentication.getName() or directly using surveyId with security service
        String currentUsername = authentication.getName();
        logger.info("User {} attempting to delete survey ID: {}", currentUsername, surveyId);
        // ResourceNotFoundException will be thrown by the service if not found / not owned by this user (via surveySecurityService)
        surveyService.deleteSurvey(surveyId, currentUsername); // Assuming deleteSurvey in service handles actual deletion
        logger.info("Survey ID {} deleted by user {}", surveyId, currentUsername);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    // This maps to "/api/surveys" as per @RequestMapping at class level.
    @PreAuthorize("isAuthenticated()") // Ensures user is authenticated
    public List<SurveyDTO> getSurveysForCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        List<Survey> surveys = surveyRepository.findByOwnerId(user.getId()); // Explicitly fetching owned surveys.
                                                                              // Change to surveyService.getSurveysByUsername(username)
                                                                              // if that service method has more complex logic (e.g. includes shared)
        
        List<SurveyDTO> surveyDTOs = surveys.stream()
                                           .map(this::convertToSurveyDTO)
                                           .collect(Collectors.toList());
        logger.info("User {} fetched {} surveys via getSurveysForCurrentUser.", username, surveyDTOs.size());
        return surveyDTOs;
    }

}
