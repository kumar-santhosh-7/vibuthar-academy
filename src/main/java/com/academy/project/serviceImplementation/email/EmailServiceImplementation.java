package com.academy.project.serviceImplementation.email;

import com.academy.project.entity.intrest.Interest;
import com.academy.project.service.emailService.IntrestEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


// @Service // SMTP mail temporarily disabled
@RequiredArgsConstructor
public class EmailServiceImplementation implements IntrestEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${academy.contact.email}")
    private String contactEmail;

    @Override
    public void sendInterestNotification(Interest interest) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(contactEmail);

        message.setSubject(
                "New Course Interest - " + interest.getCourseOfInterest()
        );

        message.setText(
                "New interested candidate received.\n\n" +

                        "Username: " + interest.getUsername() + "\n" +
                        "Email: " + interest.getEmailId() + "\n" +
                        "Mobile Number: " + interest.getMobileNumber() + "\n" +
                        "Course of Interest: " + interest.getCourseOfInterest() + "\n\n" +

                        "Description:\n" +
                        interest.getDescription()
        );

        mailSender.send(message);
    }
}