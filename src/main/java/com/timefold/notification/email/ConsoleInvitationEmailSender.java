package com.timefold.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class ConsoleInvitationEmailSender implements InvitationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleInvitationEmailSender.class);

    @Override
    public void sendContributorInvitation(
            String toEmail,
            String inviterName,
            String inviteLink,
            int validDays
    ) {
        log.info("""
                ============= [DEV EMAIL: CONTRIBUTOR INVITATION] =============
                To: {}
                Invited By: {}
                Valid For: {} days
                Invitation URL: {}
                ===============================================================
                """, toEmail, inviterName, validDays, inviteLink);
    }

}