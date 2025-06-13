package com.google.authservice.service;

import com.google.authservice.model.Survey;
import com.google.authservice.repository.SurveyRepository;
import com.google.authservice.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;

    @Autowired
    public SurveyService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    @Transactional(readOnly = true)
    public List<Survey> getSurveysByUserId(String userId) {
        return surveyRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Survey> getSurveyByIdAndUserId(Long surveyId, String userId) {
        return surveyRepository.findByIdAndUserId(surveyId, userId);
    }

    @Transactional(readOnly = true)
    public Optional<Survey> getSurveyById(Long surveyId) { // Potentially for admin or internal use
        return surveyRepository.findById(surveyId);
    }

    @Transactional
    public Survey createSurvey(Survey survey, String userId) {
        survey.setUserId(userId);
        if (survey.getStatus() == null || survey.getStatus().isEmpty()) {
            survey.setStatus("draft"); // Default status
        }
        return surveyRepository.save(survey);
    }

    @Transactional
    public Survey updateSurvey(Long surveyId, Survey surveyDetails, String userId) {
        Survey existingSurvey = surveyRepository.findByIdAndUserId(surveyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", "id", surveyId + " for user " + userId));

        existingSurvey.setTitle(surveyDetails.getTitle());
        existingSurvey.setDescription(surveyDetails.getDescription());
        existingSurvey.setQuestionsJson(surveyDetails.getQuestionsJson());
        existingSurvey.setStatus(surveyDetails.getStatus());
        // The userId is not updated to maintain ownership.
        // Timestamps (createdAt, updatedAt) are typically handled by JPA/Hibernate @CreationTimestamp and @UpdateTimestamp

        return surveyRepository.save(existingSurvey);
    }

    @Transactional
    public void deleteSurvey(Long surveyId, String userId) {
        Survey existingSurvey = surveyRepository.findByIdAndUserId(surveyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", "id", surveyId + " for user " + userId));
        surveyRepository.delete(existingSurvey);
    }
}
