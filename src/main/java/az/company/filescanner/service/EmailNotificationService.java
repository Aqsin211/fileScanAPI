package az.company.filescanner.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String adminEmail;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        Dotenv dotenv = Dotenv.load();
        this.senderEmail = dotenv.get("EMAIL_USERNAME");
        this.adminEmail = dotenv.get("EMAIL_ADMIN");
    }

    public void sendVirusAlert(String fileName, String virusName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(adminEmail);
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
