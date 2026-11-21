package com.timefold.user.dto;

public record UserDashboardSummary(
        String displayName,
        String email,
        String timezone,
        long personalCount,
        long sharedCount,
        long receivedCount,
        long readyCount,
        long sealedCount,
        long openedCount,
        long pendingInvitationsCount
) {}