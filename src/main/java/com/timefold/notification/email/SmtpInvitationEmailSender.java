package com.timefold.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class SmtpInvitationEmailSender implements InvitationEmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpInvitationEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@timefold.hari-world.top}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendContributorInvitation(
            String toEmail,
            String inviterName,
            String inviteLink,
            int validDays
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(inviterName + " invited you to contribute to a TimeFold capsule");
        message.setText("Hello,\n\n"
                + inviterName + " has invited you to contribute a message to a shared TimeFold time capsule.\n\n"
                + "Open the link below to view and respond to your invitation (valid for " + validDays + " days):\n\n"
                + inviteLink + "\n\n"
                + "If you do not yet have a TimeFold account, register using this exact email address first, then open the link.");
        mailSender.send(message);
    }

}