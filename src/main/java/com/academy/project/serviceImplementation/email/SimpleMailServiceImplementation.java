package com.academy.project.serviceImplementation.email;

import com.academy.project.service.emailService.IntrestEmailService;
import com.academy.project.service.emailService.SimpleMailService;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
// @Service // SMTP mail temporarily disabled
public class SimpleMailServiceImplementation implements SimpleMailService {

    private final JavaMailSender mailSender;

    public SimpleMailServiceImplementation(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("your-email@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
