package mcc.survey.creator.repository;

import mcc.survey.creator.model.Service;
import mcc.survey.creator.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByNameAndService(String name, Service service);
    List<Team> findByService(Service service);
    Boolean existsByNameAndService(String name, Service service);
}
