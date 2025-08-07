package az.company.filescanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${admin-email:}")
    private String adminEmails; // comma-separated list of emails

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVirusAlert(String fileName, String virusName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);

        // Split adminEmails by comma, trim whitespace, and send to all
        String[] recipients = adminEmails.split("\\s*,\\s*");
        message.setTo(recipients);

        message.setSubject("Virus Detected in File Upload");
        message.setText(String.format("""
                    Alert: A virus has been detected in an uploaded file.
                
                    File Name: %s
                    Virus Detected: %s
                    Timestamp: %s
                
                    Immediate action is recommended.
                """, fileName, virusName, java.time.LocalDateTime.now()));

        mailSender.send(message);
    }
}
