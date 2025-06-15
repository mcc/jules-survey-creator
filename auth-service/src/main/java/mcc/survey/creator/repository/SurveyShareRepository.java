package mcc.survey.creator.repository;

import mcc.survey.creator.model.SurveyShare;
import mcc.survey.creator.model.Survey;
import mcc.survey.creator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyShareRepository extends JpaRepository<SurveyShare, Long> {
    Optional<SurveyShare> findBySurveyAndUser(Survey survey, User user);
    List<SurveyShare> findBySurvey(Survey survey);
    List<SurveyShare> findByUser(User user);
    void deleteBySurveyAndUser(Survey survey, User user);
}
