package mcc.survey.creator.controller;

import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<Survey> createSurvey(@RequestBody Survey survey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        survey.setOwner(user);
        Survey savedSurvey = surveyRepository.save(survey);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSurvey);
    }

    @GetMapping("/")
    public ResponseEntity<List<Survey>> getAllSurveysForUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Survey> surveys = surveyRepository.findByOwnerId(user.getId());
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Survey> getSurveyById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            // Check if the user is the owner or if the survey is shared with the user
            if (survey.getOwner().getId().equals(user.getId()) || survey.getSharedWithUsers().contains(user)) {
                return ResponseEntity.ok(survey);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/share/{userId}")
    public ResponseEntity<Survey> shareSurvey(@PathVariable Long id, @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User owner = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        Optional<User> userToShareWithOptional = userRepository.findById(userId);

        if (surveyOptional.isPresent() && userToShareWithOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            User userToShareWith = userToShareWithOptional.get();

            // Check if the authenticated user is the owner of the survey
            if (!survey.getOwner().getId().equals(owner.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Prevent sharing with oneself
            if (owner.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or a specific error message
            }

            survey.getSharedWithUsers().add(userToShareWith);
            surveyRepository.save(survey);
            return ResponseEntity.ok(survey);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/unshare/{userId}")
    public ResponseEntity<Survey> unshareSurvey(@PathVariable Long id, @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User owner = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        Optional<User> userToUnshareOptional = userRepository.findById(userId);

        if (surveyOptional.isPresent() && userToUnshareOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            User userToUnshare = userToUnshareOptional.get();

            // Check if the authenticated user is the owner of the survey
            if (!survey.getOwner().getId().equals(owner.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            survey.getSharedWithUsers().remove(userToUnshare);
            surveyRepository.save(survey);
            return ResponseEntity.ok(survey);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<Survey> updateSurvey(@PathVariable Long id, @RequestBody Survey surveyDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            if (survey.getOwner().getId().equals(user.getId())) {
                survey.setSurveyMode(surveyDetails.getSurveyMode());
                survey.setDataClassification(surveyDetails.getDataClassification());
                survey.setStatus(surveyDetails.getStatus());
                survey.setSurveyJson(surveyDetails.getSurveyJson());
                Survey updatedSurvey = surveyRepository.save(survey);
                return ResponseEntity.ok(updatedSurvey);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userRepository.findByUsername(currentPrincipalName).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Survey> surveyOptional = surveyRepository.findById(id);
        if (surveyOptional.isPresent()) {
            Survey survey = surveyOptional.get();
            if (survey.getOwner().getId().equals(user.getId())) {
                surveyRepository.delete(survey);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
