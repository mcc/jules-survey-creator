package mcc.survey.creator.repository;

import mcc.survey.creator.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    List<Survey> findByOwnerId(Long ownerId);

    Optional<Survey> findByIdAndOwnerId(Long id, Long ownerId);
}
