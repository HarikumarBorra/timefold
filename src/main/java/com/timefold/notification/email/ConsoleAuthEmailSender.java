package com.timefold.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class ConsoleAuthEmailSender implements AuthEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAuthEmailSender.class);

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String verificationLink) {
        log.info("""
                ================ [DEV EMAIL: VERIFY ACCOUNT] ================
                To: {} ({})
                Verification URL: {}
                =============================================================
                """, toEmail, fullName, verificationLink);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        log.info("""
                ================ [DEV EMAIL: PASSWORD RESET] ================
                To: {} ({})
                Reset URL: {}
                =============================================================
                """, toEmail, fullName, resetLink);
    }

}