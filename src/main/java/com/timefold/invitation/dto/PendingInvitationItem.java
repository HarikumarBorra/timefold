package com.timefold.invitation.dto;

public record PendingInvitationItem(
        Long invitationId,
        String capsulePublicId,
        String capsuleTitle,
        String capsuleTheme,
        String inviterName,
        String unlockAtFormatted,
        String expiresAtFormatted
) {
}