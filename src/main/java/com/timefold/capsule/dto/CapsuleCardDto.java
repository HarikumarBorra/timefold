package com.timefold.capsule.dto;

import com.timefold.common.enums.CapsuleStatus;
import com.timefold.common.enums.CapsuleType;

public record CapsuleCardDto(
        String publicId,
        String title,
        String description,
        String theme,
        CapsuleType capsuleType,
        CapsuleStatus effectiveStatus,
        String unlockAtFormatted,
        String unlockAtUtcIso,
        String timezone,
        boolean openedByCurrentUser
) {
}