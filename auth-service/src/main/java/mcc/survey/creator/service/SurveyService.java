package mcc.survey.creator.service;

import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SurveyService {
    Logger logger = org.slf4j.LoggerFactory.getLogger(SurveyService.class);
    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Autowired
    public SurveyService(SurveyRepository surveyRepository, UserRepository userRepository) {
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Survey> getSurveysByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " not found"));
        return surveyRepository.findByOwnerId(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Survey> getSurveyByIdAndOwnerId(Long surveyId, Long ownerId) {
        return surveyRepository.findByIdAndOwnerId(surveyId, ownerId);
    }

    @Transactional(readOnly = true)
    public Optional<Survey> getSurveyByIdAndUsername(Long surveyId, String username) {
        logger.info("Fetching survey with ID: " + surveyId + " for user: " + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " not found"));
        Long ownerId = user.getId();
        logger.info(ownerId + " is the ownerId for surveyId: " + surveyId);
        return surveyRepository.findByIdAndOwnerId(surveyId, ownerId);
    }

    @Transactional(readOnly = true)
    public Optional<Survey> getSurveyById(Long surveyId) { // Potentially for admin or internal use
        return surveyRepository.findById(surveyId);
    }

    @Transactional
    public Survey createSurvey(Survey survey, String userName) {
        survey.setOwner(userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + userName + " not found")));
        if (survey.getStatus() == null || survey.getStatus().isEmpty()) {
            survey.setStatus("draft"); // Default status
        }
        return surveyRepository.save(survey);
    }

    @Transactional
    public Survey updateSurvey(Long surveyId, Survey surveyDetails, String username) {
        Survey existingSurvey = this.getSurveyByIdAndUsername(surveyId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Survey"));

        existingSurvey.setTitle(surveyDetails.getTitle());
        existingSurvey.setDescription(surveyDetails.getDescription());
        existingSurvey.setQuestionsJson(surveyDetails.getQuestionsJson());
        existingSurvey.setStatus(surveyDetails.getStatus());
        // The userId is not updated to maintain ownership.
        // Timestamps (createdAt, updatedAt) are typically handled by JPA/Hibernate @CreationTimestamp and @UpdateTimestamp

        return surveyRepository.save(existingSurvey);
    }

    @Transactional
    public void deleteSurvey(Long surveyId, String username) {
        Survey existingSurvey = this.getSurveyByIdAndUsername(surveyId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Survey"));
        surveyRepository.delete(existingSurvey);
    }
}
