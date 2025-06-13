package mcc.survey.creator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendPasswordResetEmail(String email, String token) {
        // This is a stub. In a real application, this would send an email.
        // For now, we'll just log the information.
        String resetLink = "http://localhost:8081/reset-password?token=" + token; // Assuming frontend runs on 8081 and has this path
        logger.info("Simulating sending password reset email to: {}", email);
        logger.info("Password reset link: {}", resetLink);
    }
}
