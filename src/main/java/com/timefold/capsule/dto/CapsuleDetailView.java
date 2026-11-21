package com.timefold.capsule.dto;

import com.timefold.common.enums.CapsuleInvitationStatus;
import com.timefold.common.enums.CapsuleStatus;
import com.timefold.common.enums.CapsuleType;
import com.timefold.common.enums.MemberRole;
import com.timefold.common.enums.VisibilityPolicy;
import java.util.List;
import java.util.Set;

public record CapsuleDetailView(
        String publicId,
        String title,
        String description,
        String theme,
        CapsuleType capsuleType,
        VisibilityPolicy visibilityPolicy,
        CapsuleStatus status,
        boolean isOwner,
        boolean isShared,
        boolean openedByCurrentUser,
        boolean canEdit,
        boolean canSeal,
        boolean canCancel,
        boolean canDeleteDraft,
        boolean canOpenNow,
        boolean canSubmitOwnContribution,
        boolean hasOwnContribution,
        String ownContributionTitle,
        String ownContributionMessage,
        int totalSubmittedContributions,
        boolean showMessage,
        String decryptedMessage,
        String unlockAtFormatted,
        String unlockAtUtcIso,
        String timezone,
        String sealedAtFormatted,
        String unlockedAtFormatted,
        String cancelledAtFormatted,
        String createdAtFormatted,
        List<MemberItem> members,
        List<InvitationItem> invitations,
        List<DraftContributionSummary> draftContributions,
        List<RevealedContributionItem> visibleContributions,
        List<OpenHistoryItem> openHistory
) {
    public record MemberItem(
            Long memberId,
            Long userId,
            String fullName,
            String email,
            MemberRole role,
            boolean canEditBeforeSeal,
            boolean hasSubmitted,
            String joinedAtFormatted
    ) {
    }

    public record InvitationItem(
            Long invitationId,
            String inviteeEmail,
            CapsuleInvitationStatus status,
            String expiresAtFormatted
    ) {
    }

    public record DraftContributionSummary(
            String contributionPublicId,
            Long contributorUserId,
            String contributorName,
            String title,
            String updatedAtFormatted,
            Set<Long> allowedUserIds
    ) {
    }

    public record RevealedContributionItem(
            String contributionPublicId,
            String contributorName,
            boolean ownContribution,
            String title,
            String decryptedMessage,
            String submittedAtFormatted
    ) {
    }

    public record OpenHistoryItem(
            String viewerName,
            String firstOpenedFormatted,
            String lastOpenedFormatted,
            int openCount
    ) {
    }

}