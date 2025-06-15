package mcc.survey.creator.controller; // Corrected package name

import mcc.survey.creator.dto.SharedUserDTO; // Corrected package name
import mcc.survey.creator.dto.ShareSurveyRequest; // Corrected package name
import mcc.survey.creator.service.SurveySharingService; // Corrected package name
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
        List<SharedUserDTO> sharedUsers = surveySharingService.getSharedUsers(surveyId);
        return ResponseEntity.ok(sharedUsers);
    }

    @PostMapping("/{surveyId}/share")
    public ResponseEntity<?> shareSurvey(
            @PathVariable String surveyId,
            @RequestBody ShareSurveyRequest request) {
        surveySharingService.shareSurveyWithUser(surveyId, request.getUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{surveyId}/unshare")
    public ResponseEntity<?> unshareSurvey(
            @PathVariable String surveyId,
            @RequestBody ShareSurveyRequest request) { // Assuming userId to unshare comes in body
        surveySharingService.unshareSurveyWithUser(surveyId, request.getUserId());
        return ResponseEntity.ok().build();
    }
}
