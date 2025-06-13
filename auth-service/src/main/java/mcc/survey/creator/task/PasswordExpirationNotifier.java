package mcc.survey.creator.task;

import mcc.survey.creator.model.User;
import mcc.survey.creator.repository.UserRepository;
import mcc.survey.creator.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class PasswordExpirationNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordExpirationNotifier.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${password.expiry.notification.days}")
    private int notificationDays;

    // Run once a day at 2 AM, for example
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkAndNotifyPasswordExpirations() {
        log.info("Running password expiration check...");
        LocalDate today = LocalDate.now();
        LocalDate notificationDate = today.plusDays(notificationDays);

        // Find users whose passwords expire between today (exclusive, as they might have already been caught by login check)
        // and the notificationDate (inclusive).
        // Also, ensure the user has an email address and is active.
        List<User> usersToNotify = userRepository.findByPasswordExpirationDateBetweenAndIsActiveTrueAndEmailIsNotNull(today, notificationDate);

        if (usersToNotify.isEmpty()) {
            log.info("No users found with passwords expiring soon.");
            return;
        }

        log.info("Found {} users with passwords expiring soon. Sending notifications...", usersToNotify.size());
        for (User user : usersToNotify) {
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                log.warn("User {} (ID: {}) has a password expiring on {} but has no email address. Skipping notification.", user.getUsername(), user.getId(), user.getPasswordExpirationDate());
                continue;
            }
            String subject = "Password Expiration Reminder";
            String text = String.format(
                "Dear %s,\n\nYour password is scheduled to expire on %s.\n\nPlease change your password before then to maintain access to your account.\n\nThank you,\nThe System",
                user.getUsername(),
                user.getPasswordExpirationDate().toString()
            );
            emailService.sendSimpleMessage(user.getEmail(), subject, text);
        }
        log.info("Finished sending password expiration notifications.");
    }
}
