package mcc.survey.creator.repository;

import mcc.survey.creator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Optional<User> findByEmail(String email); // findByEmail already exists, that's good.
    Boolean existsByEmail(String email); // Assuming this was added or already exists

    // Method for the password expiration notifier
    List<User> findByPasswordExpirationDateBetweenAndIsActiveTrueAndEmailIsNotNull(LocalDate startDate, LocalDate endDate);
}
