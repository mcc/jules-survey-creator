package com.google.authservice.controller;

import com.google.authservice.dto.SharedUserDTO;
import com.google.authservice.dto.ShareSurveyRequest;
import com.google.authservice.service.SurveySharingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
public class SurveySharingController {

    private final SurveySharingService surveySharingService;

    @Autowired
    public SurveySharingController(SurveySharingService surveySharingService) {
        this.surveySharingService = surveySharingService;
    }

    @GetMapping("/{surveyId}/shared-users")
    public ResponseEntity<List<SharedUserDTO>> getSharedUsers(@PathVariable String surveyId) {
        // TODO: Add validation for surveyId and proper error handling
        List<SharedUserDTO> sharedUsers = surveySharingService.getSharedUsers(surveyId);
        return ResponseEntity.ok(sharedUsers);
    }

    @PostMapping("/{surveyId}/share")
    public ResponseEntity<?> shareSurvey(
            @PathVariable String surveyId,
            @RequestBody ShareSurveyRequest request) {
        // TODO: Add validation for surveyId and request body, and proper error handling
        surveySharingService.shareSurveyWithUser(surveyId, request.getUserId());
        return ResponseEntity.ok().build(); // Consider returning the updated list or the shared user
    }

    @DeleteMapping("/{surveyId}/unshare")
    public ResponseEntity<?> unshareSurvey(
            @PathVariable String surveyId,
            @RequestBody ShareSurveyRequest request) { // Assuming userId to unshare comes in body
        // Alternative: @RequestParam String userId or @PathVariable String userId if it's part of the URL
        // TODO: Add validation for surveyId and request body, and proper error handling
        surveySharingService.unshareSurveyWithUser(surveyId, request.getUserId());
        return ResponseEntity.ok().build(); // Consider returning a confirmation or the updated list
    }
}
