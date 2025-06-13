package mcc.survey.creator.service;

import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.SurveyRepository;
import mcc.survey.creator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("surveySecurityService") // Bean name for use in SpEL
public class SurveySecurityService {

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Checks if the authenticated user is the owner of the survey.
     */
    public boolean isOwner(Authentication authentication, Long surveyId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return false;
        }
        User currentUser = userOptional.get();

        Optional<Survey> surveyOptional = surveyRepository.findById(surveyId);
        if (surveyOptional.isEmpty()) {
            return false; // Or throw an exception, or handle as per requirements
        }
        Survey survey = surveyOptional.get();
        return survey.getOwner() != null && survey.getOwner().getId().equals(currentUser.getId());
    }

    /**
     * Checks if the authenticated user is the owner of the survey or the survey is shared with them.
     * This can be used for read-only access where shared users can also view.
     */
    public boolean isOwnerOrSharedUser(Authentication authentication, Long surveyId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return false;
        }
        User currentUser = userOptional.get();

        Optional<Survey> surveyOptional = surveyRepository.findById(surveyId);
        if (surveyOptional.isEmpty()) {
            return false;
        }
        Survey survey = surveyOptional.get();

        // Check if owner
        if (survey.getOwner() != null && survey.getOwner().getId().equals(currentUser.getId())) {
            return true;
        }
        // Check if shared with the user
        // The Survey entity needs a getSharedWithUsers() method that returns a collection of User objects.
        // Assuming Survey.getSharedWithUsers() returns Set<User>
        return survey.getSharedWithUsers().stream().anyMatch(sharedUser -> sharedUser.getId().equals(currentUser.getId()));
    }
}
