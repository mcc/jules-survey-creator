package com.google.authservice.controller;

import com.google.authservice.model.Survey;
import com.google.authservice.service.SurveyService;
// Assuming UserService is not used for now as per instructions for a mock getCurrentUserId()
// import com.google.authservice.service.UserService;
import com.google.authservice.exception.ResourceNotFoundException; // Though handled by @ResponseStatus

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.core.annotation.AuthenticationPrincipal; // For later: to get user details
// import org.springframework.security.core.userdetails.UserDetails; // For later

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;
    // private final UserService userService; // Inject if using userService.getCurrentUserId()

    @Autowired
    public SurveyController(SurveyService surveyService /*, UserService userService */) {
        this.surveyService = surveyService;
        // this.userService = userService;
    }

    // Placeholder for getting current user ID. Replace with Spring Security context later.
    private String getCurrentUserId() {
        // In a real application, this would be retrieved from the Spring Security context,
        // e.g., using @AuthenticationPrincipal or SecurityContextHolder.getContext().getAuthentication()
        // For example:
        // Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // if (principal instanceof UserDetails) {
        //     return ((UserDetails) principal).getUsername(); // Or a custom User object with an ID
        // } else if (principal instanceof String) {
        //     return (String) principal;
        // }
        // return "anonymousUser"; // Or throw an exception if user must be authenticated
        return "mock-user-id-123"; // Hardcoded for now
    }

    @GetMapping
    public List<Survey> getSurveysForCurrentUser() {
        String userId = getCurrentUserId();
        return surveyService.getSurveysByUserId(userId);
    }

    @PostMapping
    public ResponseEntity<Survey> createSurvey(@Valid @RequestBody Survey survey) {
        String userId = getCurrentUserId();
        Survey createdSurvey = surveyService.createSurvey(survey, userId);
        return new ResponseEntity<>(createdSurvey, HttpStatus.CREATED);
    }

    @GetMapping("/{surveyId}")
    public ResponseEntity<Survey> getSurveyById(@PathVariable Long surveyId) {
        String userId = getCurrentUserId();
        Optional<Survey> surveyOptional = surveyService.getSurveyByIdAndUserId(surveyId, userId);
        return surveyOptional
                .map(survey -> new ResponseEntity<>(survey, HttpStatus.OK))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{surveyId}")
    public ResponseEntity<Survey> updateSurvey(@PathVariable Long surveyId, @Valid @RequestBody Survey surveyDetails) {
        String userId = getCurrentUserId();
        // ResourceNotFoundException will be thrown by the service if not found / not owned
        Survey updatedSurvey = surveyService.updateSurvey(surveyId, surveyDetails, userId);
        return new ResponseEntity<>(updatedSurvey, HttpStatus.OK);
    }

    @DeleteMapping("/{surveyId}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long surveyId) {
        String userId = getCurrentUserId();
        // ResourceNotFoundException will be thrown by the service if not found / not owned
        surveyService.deleteSurvey(surveyId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
