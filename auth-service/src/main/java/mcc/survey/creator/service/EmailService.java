package mcc.survey.creator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.from.address}")
    private String fromAddress;

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage());
            // In a real application, you might want to throw a custom exception
            // or handle this more gracefully, e.g., by queueing the email for retry.
        }
    }
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendPasswordResetEmail(String email, String token) {
        // This is a stub. In a real application, this would send an email.
        // For now, we'll just log the information.
        String resetLink = "http://localhost:8081/reset-password?token=" + token; // Assuming frontend runs on 8081 and has this path
        logger.info("Simulating sending password reset email to: {}", email);
        logger.info("Password reset link: {}", resetLink);
    }
}
