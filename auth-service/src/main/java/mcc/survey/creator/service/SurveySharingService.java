package mcc.survey.creator.service;

import mcc.survey.creator.dto.SharedUserDTO;
import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.model.SurveyShare;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.repository.SurveyShareRepository;
import mcc.survey.creator.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Added for @Transactional

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
// ArrayList is not explicitly used in the final version of the methods, but might be useful for callers or future DTOs.
// Keeping it for now, can be removed if a linter/optimizer suggests.
import java.util.ArrayList;

@Service
public class SurveySharingService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final SurveyShareRepository surveyShareRepository;

    @Autowired
    public SurveySharingService(SurveyRepository surveyRepository,
                                UserRepository userRepository,
                                SurveyShareRepository surveyShareRepository) {
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
        this.surveyShareRepository = surveyShareRepository;
    }

    public void shareSurveyWithUser(String surveyId, String userId) {
        Survey survey = surveyRepository.findById(Long.parseLong(surveyId)) // Assuming surveyId is Long
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + surveyId));
        User user = userRepository.findById(Long.parseLong(userId)) // Assuming userId is Long
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if already shared
        Optional<SurveyShare> existingShare = surveyShareRepository.findBySurveyAndUser(survey, user);
        if (existingShare.isPresent()) {
            // Optionally, log a message or return a specific response
            System.out.println("Survey " + surveyId + " is already shared with user " + userId);
            return;
        }

        SurveyShare surveyShare = new SurveyShare();
        surveyShare.setSurvey(survey);
        surveyShare.setUser(user);
        surveyShareRepository.save(surveyShare);
        System.out.println("Service: Successfully shared surveyId: " + surveyId + " with userId: " + userId);
    }

    @Transactional // Important for delete operations
    public void unshareSurveyWithUser(String surveyId, String userId) {
        Survey survey = surveyRepository.findById(Long.parseLong(surveyId))
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + surveyId));
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<SurveyShare> existingShare = surveyShareRepository.findBySurveyAndUser(survey, user);
        if (existingShare.isEmpty()) {
            throw new ResourceNotFoundException("Survey " + surveyId + " is not shared with user " + userId);
        }

        // surveyShareRepository.deleteBySurveyAndUser(survey, user); // This custom query method needs to be implemented or use .delete()
        surveyShareRepository.delete(existingShare.get()); // Standard JpaRepository method
        System.out.println("Service: Successfully unshared surveyId: " + surveyId + " from userId: " + userId);
    }

    public List<SharedUserDTO> getSharedUsers(String surveyId) {
        Survey survey = surveyRepository.findById(Long.parseLong(surveyId))
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + surveyId));

        List<SurveyShare> shares = surveyShareRepository.findBySurvey(survey);

        return shares.stream()
                .map(SurveyShare::getUser)
                .map(user -> new SharedUserDTO(user.getId().toString(), user.getUsername(), user.getEmail())) // Adjust DTO constructor as needed
                .collect(Collectors.toList());
    }
}
