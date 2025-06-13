package com.google.authservice.repository;

import com.google.authservice.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    /**
     * Finds all surveys associated with a specific user ID.
     *
     * @param userId The ID of the user.
     * @return A list of surveys belonging to the user.
     */
    List<Survey> findByUserId(String userId);

    /**
     * Finds a survey by its ID and user ID.
     * This can be useful to ensure a user is accessing their own survey.
     *
     * @param id The ID of the survey.
     * @param userId The ID of the user.
     * @return An Optional containing the survey if found and owned by the user, otherwise empty.
     */
    Optional<Survey> findByIdAndUserId(Long id, String userId);
}
