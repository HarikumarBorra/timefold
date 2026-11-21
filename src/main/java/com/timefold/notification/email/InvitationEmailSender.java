package com.timefold.notification.email;

public interface InvitationEmailSender {

    void sendContributorInvitation(
            String toEmail,
            String inviterName,
            String inviteLink,
            int validDays
    );

}