package mcc.survey.creator.controller;

import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/")
    @PreAuthorize("hasAuthority('OP_CREATE_SURVEY')")
    public ResponseEntity<Survey> createSurvey(@RequestBody Survey survey, Authentication authentication) {
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for token"));
        survey.setOwner(user);
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

    @GetMapping("/{id}")
    @PreAuthorize("(hasAuthority('OP_VIEW_OWN_SURVEY') and @surveySecurityService.isOwnerOrSharedUser(authentication, #id)) or hasAuthority('OP_VIEW_ALL_SURVEYS')")
    public ResponseEntity<Survey> getSurveyById(@PathVariable Long id) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isPresent()) {
            return ResponseEntity.ok(surveyOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            survey.getSharedWithUsers().add(userToShareWith);
            surveyRepository.save(survey);
            return ResponseEntity.ok(survey);
        } else {
            return ResponseEntity.notFound().build();
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
            return ResponseEntity.notFound().build();
        }
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OP_EDIT_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Survey> updateSurvey(@PathVariable Long id, @RequestBody Survey surveyDetails) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            // Ownership is checked by @PreAuthorize
            survey.setSurveyMode(surveyDetails.getSurveyMode());
            survey.setDataClassification(surveyDetails.getDataClassification());
            survey.setStatus(surveyDetails.getStatus());
            survey.setSurveyJson(surveyDetails.getSurveyJson());
            Survey updatedSurvey = surveyRepository.save(survey);
            return ResponseEntity.ok(updatedSurvey);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OP_DELETE_OWN_SURVEY') and @surveySecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isPresent()) {
            // Ownership is checked by @PreAuthorize
            surveyRepository.delete(surveyOptional.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
