package com.timefold.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class SmtpAuthEmailSender implements AuthEmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpAuthEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@timefold.hari-world.top}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your TimeFold account");
        message.setText("Hi " + fullName + ",\n\n"
                + "Welcome to TimeFold! Please verify your email address using the link below (valid for 24 hours):\n\n"
                + verificationLink + "\n\n"
                + "If you did not create this account, you can safely ignore this email.");
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your TimeFold password");
        message.setText("Hi " + fullName + ",\n\n"
                + "Use the link below to reset your TimeFold password (valid for 30 minutes):\n\n"
                + resetLink + "\n\n"
                + "If you did not request a password reset, you can safely ignore this email.");
        mailSender.send(message);
    }

}