package com.timefold.capsule.service;

import com.timefold.audit.service.AuditService;
import com.timefold.capsule.dto.ContributionForm;
import com.timefold.capsule.dto.SharedCapsuleForm;
import com.timefold.capsule.entity.Capsule;
import com.timefold.capsule.entity.CapsuleMember;
import com.timefold.capsule.repository.CapsuleMemberRepository;
import com.timefold.capsule.repository.CapsuleRepository;
import com.timefold.common.enums.CapsuleInvitationStatus;
import com.timefold.common.enums.CapsuleStatus;
import com.timefold.common.enums.CapsuleType;
import com.timefold.common.enums.ContributionStatus;
import com.timefold.common.enums.ContributionVisibility;
import com.timefold.common.enums.InvitationStatus;
import com.timefold.common.enums.MemberRole;
import com.timefold.common.enums.VisibilityPolicy;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.exception.ResourceNotFoundException;
import com.timefold.common.util.MessageEncryptionService;
import com.timefold.common.util.TokenUtil;
import com.timefold.config.AppProperties;
import com.timefold.contribution.entity.CapsuleContribution;
import com.timefold.contribution.entity.CapsuleContributionVisibility;
import com.timefold.contribution.entity.CapsuleContributionVisibilityId;
import com.timefold.contribution.repository.CapsuleContributionRepository;
import com.timefold.contribution.repository.CapsuleContributionVisibilityRepository;
import com.timefold.invitation.dto.PendingInvitationItem;
import com.timefold.invitation.entity.CapsuleInvitation;
import com.timefold.invitation.repository.CapsuleInvitationRepository;
import com.timefold.notification.email.InvitationEmailSender;
import com.timefold.user.entity.Role;
import com.timefold.user.entity.User;
import com.timefold.user.entity.UserProfile;
import com.timefold.user.repository.UserProfileRepository;
import com.timefold.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedCapsuleService {

    private static final int INVITATION_VALID_DAYS = 7;
    private static final long MAX_DAILY_INVITATIONS_PER_USER = 25L;
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a z");
    private static final DateTimeFormatter INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final CapsuleRepository capsuleRepository;
    private final CapsuleMemberRepository capsuleMemberRepository;
    private final CapsuleContributionRepository contributionRepository;
    private final CapsuleContributionVisibilityRepository visibilityRepository;
    private final CapsuleInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MessageEncryptionService encryptionService;
    private final InvitationEmailSender invitationEmailSender;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public SharedCapsuleService(
            CapsuleRepository capsuleRepository,
            CapsuleMemberRepository capsuleMemberRepository,
            CapsuleContributionRepository contributionRepository,
            CapsuleContributionVisibilityRepository visibilityRepository,
            CapsuleInvitationRepository invitationRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            MessageEncryptionService encryptionService,
            InvitationEmailSender invitationEmailSender,
            AuditService auditService,
            AppProperties appProperties
    ) {
        this.capsuleRepository = capsuleRepository;
        this.capsuleMemberRepository = capsuleMemberRepository;
        this.contributionRepository = contributionRepository;
        this.visibilityRepository = visibilityRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.encryptionService = encryptionService;
        this.invitationEmailSender = invitationEmailSender;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public SharedCapsuleForm prepareNewSharedForm(Long userId) {
        String userTz = resolveUserTimezone(userId);
        ZoneId zoneId = ZoneId.of(userTz);
        ZonedDateTime defaultUnlock = ZonedDateTime.now(zoneId).plusDays(14).withSecond(0).withNano(0);

        SharedCapsuleForm form = new SharedCapsuleForm();
        form.setTimezone(userTz);
        form.setUnlockLocalDateTime(INPUT_FORMATTER.format(defaultUnlock));
        form.setTheme("CELEBRATION");
        form.setVisibilityPolicy(VisibilityPolicy.ALL_MEMBERS);
        return form;
    }

    @Transactional
    public String createSharedCapsule(Long ownerUserId, SharedCapsuleForm form, String ip, String userAgent) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        ZoneId zoneId = parseZoneId(form.getTimezone());
        Instant unlockAtUtc = parseFutureUnlockInstant(form.getUnlockLocalDateTime(), zoneId);

        Capsule capsule = new Capsule();
        capsule.setOwner(owner);
        capsule.setTitle(form.getTitle().trim());
        capsule.setDescription(trimToNull(form.getDescription()));
        capsule.setTheme(normalizeTheme(form.getTheme()));
        capsule.setCapsuleType(CapsuleType.SHARED);
        capsule.setVisibilityPolicy(form.getVisibilityPolicy());
        capsule.setStatus(CapsuleStatus.DRAFT);
        capsule.setUnlockAtUtc(unlockAtUtc);
        capsule.setTimezone(zoneId.getId());
        capsule = capsuleRepository.save(capsule);

        CapsuleMember ownerMember = new CapsuleMember();
        ownerMember.setCapsule(capsule);
        ownerMember.setUser(owner);
        ownerMember.setMemberRole(MemberRole.OWNER);
        ownerMember.setCanEditBeforeSeal(true);
        ownerMember.setContributionVisibility(ContributionVisibility.ALL);
        ownerMember.setInvitationStatus(InvitationStatus.ACCEPTED);
        ownerMember.setJoinedAtUtc(Instant.now());
        capsuleMemberRepository.save(ownerMember);

        if (form.getInitialMessage() != null && !form.getInitialMessage().isBlank()) {
            MessageEncryptionService.EncryptedPayload encrypted =
                    encryptionService.encrypt(form.getInitialMessage().trim());
            CapsuleContribution contribution = new CapsuleContribution();
            contribution.setCapsule(capsule);
            contribution.setContributor(owner);
            contribution.setTitle("Owner's Note");
            contribution.setMessageContent(encrypted.ciphertextBase64());
            contribution.setEncryptionKeyVersion(encrypted.keyVersion());
            contribution.setEncryptionIv(encrypted.iv());
            contribution.setStatus(ContributionStatus.SUBMITTED);
            contributionRepository.save(contribution);
        }

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "SHARED_CAPSULE_CREATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of(
                        "visibilityPolicy", form.getVisibilityPolicy().name(),
                        "unlockAtUtc", unlockAtUtc.toString()
                )
        );

        return capsule.getPublicId();
    }

    @Transactional
    public void updateSharedSettings(
            String publicId,
            Long ownerUserId,
            VisibilityPolicy visibilityPolicy,
            String ip,
            String userAgent
    ) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwnerOfSharedDraft(capsule, ownerUserId);

        capsule.setVisibilityPolicy(visibilityPolicy);
        capsuleRepository.save(capsule);

        // Keep member visibility flags aligned with high-level policy
        ContributionVisibility memberVis = switch (visibilityPolicy) {
            case ALL_MEMBERS -> ContributionVisibility.ALL;
            case SELF_ONLY -> ContributionVisibility.OWN_ONLY;
            case OWNER_DEFINED -> ContributionVisibility.CUSTOM;
        };
        List<CapsuleMember> members = capsuleMemberRepository.findByCapsuleIdWithUser(capsule.getId());
        for (CapsuleMember member : members) {
            if (member.getMemberRole() == MemberRole.CONTRIBUTOR) {
                member.setContributionVisibility(memberVis);
                capsuleMemberRepository.save(member);
            }
        }

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "SHARED_VISIBILITY_POLICY_UPDATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("visibilityPolicy", visibilityPolicy.name())
        );
    }

    @Transactional
    public void inviteContributor(
            String publicId,
            Long ownerUserId,
            String rawInviteeEmail,
            String ip,
            String userAgent
    ) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwnerOfSharedDraft(capsule, ownerUserId);

        String inviteeEmail = rawInviteeEmail.trim().toLowerCase(Locale.ROOT);
        User owner = capsule.getOwner();
        if (owner.getEmail().equalsIgnoreCase(inviteeEmail)) {
            throw new BusinessException("You are already the owner of this shared capsule.");
        }

        Instant now = Instant.now();
        long sentLast24h = invitationRepository.countByInvitedByIdAndCreatedAtUtcAfter(
                ownerUserId, now.minus(Duration.ofHours(24)));
        if (sentLast24h >= MAX_DAILY_INVITATIONS_PER_USER) {
            throw new BusinessException("Daily invitation limit reached (25 per 24 hours). Please try again tomorrow.");
        }

        if (invitationRepository.existsByCapsuleIdAndInviteeEmailIgnoreCaseAndStatusAndExpiresAtUtcAfter(
                capsule.getId(), inviteeEmail, CapsuleInvitationStatus.PENDING, now)) {
            throw new BusinessException("An active invitation has already been sent to " + inviteeEmail + ".");
        }

        User existingUser = userRepository.findByEmailIgnoreCase(inviteeEmail).orElse(null);
        if (existingUser != null && capsuleMemberRepository.existsByCapsuleIdAndUserIdAndInvitationStatus(
                capsule.getId(), existingUser.getId(), InvitationStatus.ACCEPTED)) {
            throw new BusinessException("This person is already a contributor on this capsule.");
        }

        String rawToken = TokenUtil.generateRawToken();
        CapsuleInvitation invitation = new CapsuleInvitation();
        invitation.setCapsule(capsule);
        invitation.setInvitedBy(owner);
        invitation.setInvitedUser(existingUser);
        invitation.setInviteeEmail(inviteeEmail);
        invitation.setTokenHash(TokenUtil.sha256Hex(rawToken));
        invitation.setStatus(CapsuleInvitationStatus.PENDING);
        invitation.setExpiresAtUtc(now.plus(Duration.ofDays(INVITATION_VALID_DAYS)));
        invitationRepository.save(invitation);

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CONTRIBUTOR_INVITED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("inviteeEmail", inviteeEmail)
        );

        String inviteLink = appProperties.baseUrl() + "/invitations/token?token=" + rawToken;
        invitationEmailSender.sendContributorInvitation(
                inviteeEmail,
                owner.getFullName(),
                inviteLink,
                INVITATION_VALID_DAYS
        );
    }

    @Transactional
    public void revokeInvitation(
            String publicId,
            Long invitationId,
            Long ownerUserId,
            String ip,
            String userAgent
    ) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwnerOfSharedDraft(capsule, ownerUserId);

        CapsuleInvitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        if (!invitation.getCapsule().getId().equals(capsule.getId())) {
            throw new AccessDeniedException("Invitation does not belong to this capsule.");
        }

        int updated = invitationRepository.transitionPendingAtomically(
                invitation.getId(), CapsuleInvitationStatus.REVOKED, Instant.now());
        if (updated != 1) {
            throw new BusinessException("Only active pending invitations can be revoked.");
        }

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CONTRIBUTOR_INVITATION_REVOKED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("inviteeEmail", invitation.getInviteeEmail())
        );
    }

    @Transactional(readOnly = true)
    public List<PendingInvitationItem> listPendingInvitationsForUser(String userEmail, Long userId) {
        ZoneId userZone = ZoneId.of(resolveUserTimezone(userId));
        return invitationRepository.findActivePendingForEmail(userEmail, Instant.now()).stream()
                .filter(ci -> ci.getCapsule().getStatus() == CapsuleStatus.DRAFT)
                .map(ci -> new PendingInvitationItem(
                        ci.getId(),
                        ci.getCapsule().getPublicId(),
                        ci.getCapsule().getTitle(),
                        ci.getCapsule().getTheme(),
                        ci.getInvitedBy().getFullName(),
                        DISPLAY_FORMATTER.format(ci.getCapsule().getUnlockAtUtc().atZone(userZone)),
                        DISPLAY_FORMATTER.format(ci.getExpiresAtUtc().atZone(userZone))
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PendingInvitationItem inspectInvitationByToken(String rawToken, String currentUserEmail, Long userId) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("Invitation token is missing.");
        }
        String tokenHash = TokenUtil.sha256Hex(rawToken.trim());
        CapsuleInvitation invitation = invitationRepository.findByTokenHashWithDetails(tokenHash)
                .orElseThrow(() -> new BusinessException("This invitation link is invalid."));

        validateInvitationEligibility(invitation, currentUserEmail);

        ZoneId userZone = ZoneId.of(resolveUserTimezone(userId));
        return new PendingInvitationItem(
                invitation.getId(),
                invitation.getCapsule().getPublicId(),
                invitation.getCapsule().getTitle(),
                invitation.getCapsule().getTheme(),
                invitation.getInvitedBy().getFullName(),
                DISPLAY_FORMATTER.format(invitation.getCapsule().getUnlockAtUtc().atZone(userZone)),
                DISPLAY_FORMATTER.format(invitation.getExpiresAtUtc().atZone(userZone))
        );
    }

    @Transactional
    public String acceptInvitation(
            Long invitationId,
            Long currentUserId,
            String currentUserEmail,
            String ip,
            String userAgent
    ) {
        CapsuleInvitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        validateInvitationEligibility(invitation, currentUserEmail);

        // Lock parent capsule to prevent accepting an invitation concurrently with sealing
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(invitation.getCapsule().getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("This capsule has already been sealed and is no longer accepting contributors.");
        }

        Instant now = Instant.now();
        int updated = invitationRepository.transitionPendingAtomically(
                invitation.getId(), CapsuleInvitationStatus.ACCEPTED, now);
        if (updated != 1) {
            throw new BusinessException("This invitation has expired or was already responded to.");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        CapsuleMember member = capsuleMemberRepository.findByCapsuleIdAndUserId(capsule.getId(), currentUserId)
                .orElseGet(() -> {
                    CapsuleMember fresh = new CapsuleMember();
                    fresh.setCapsule(capsule);
                    fresh.setUser(user);
                    fresh.setMemberRole(MemberRole.CONTRIBUTOR);
                    return fresh;
                });

        member.setInvitationStatus(InvitationStatus.ACCEPTED);
        member.setCanEditBeforeSeal(true);
        member.setContributionVisibility(switch (capsule.getVisibilityPolicy()) {
            case ALL_MEMBERS -> ContributionVisibility.ALL;
            case SELF_ONLY -> ContributionVisibility.OWN_ONLY;
            case OWNER_DEFINED -> ContributionVisibility.CUSTOM;
        });
        member.setJoinedAtUtc(now);
        capsuleMemberRepository.save(member);

        auditService.record(
                currentUserId,
                Role.ROLE_USER,
                "CONTRIBUTOR_INVITATION_ACCEPTED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("invitationId", invitation.getId())
        );

        return capsule.getPublicId();
    }

    @Transactional
    public void declineInvitation(
            Long invitationId,
            Long currentUserId,
            String currentUserEmail,
            String ip,
            String userAgent
    ) {
        CapsuleInvitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        if (!invitation.getInviteeEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new AccessDeniedException("This invitation was addressed to a different email address.");
        }

        int updated = invitationRepository.transitionPendingAtomically(
                invitation.getId(), CapsuleInvitationStatus.DECLINED, Instant.now());
        if (updated != 1) {
            throw new BusinessException("This invitation is no longer pending.");
        }

        auditService.record(
                currentUserId,
                Role.ROLE_USER,
                "CONTRIBUTOR_INVITATION_DECLINED",
                "CAPSULE",
                invitation.getCapsule().getPublicId(),
                ip,
                userAgent,
                Map.of("invitationId", invitation.getId())
        );
    }

    @Transactional
    public void saveContribution(
            String publicId,
            Long currentUserId,
            ContributionForm form,
            String ip,
            String userAgent
    ) {
        // Pessimistic lock on parent capsule guarantees edits can never race with owner sealing
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));

        if (capsule.getCapsuleType() != CapsuleType.SHARED) {
            throw new BusinessException("Use the personal capsule editor for personal capsules.");
        }
        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("This shared capsule is sealed and no longer accepts contribution changes.");
        }

        CapsuleMember member = capsuleMemberRepository.findByCapsuleIdAndUserId(capsule.getId(), currentUserId)
                .filter(m -> m.getInvitationStatus() == InvitationStatus.ACCEPTED)
                .orElseThrow(() -> new AccessDeniedException("You are not an active member of this shared capsule."));

        CapsuleContribution existing = contributionRepository
                .findFirstByCapsuleIdAndContributorIdOrderByIdAsc(capsule.getId(), currentUserId)
                .orElse(null);

        if (existing != null
                && existing.getStatus() == ContributionStatus.SUBMITTED
                && member.getMemberRole() != MemberRole.OWNER
                && !member.isCanEditBeforeSeal()) {
            throw new BusinessException("The capsule owner has locked editing for your contribution.");
        }

        MessageEncryptionService.EncryptedPayload encrypted =
                encryptionService.encrypt(form.getMessageContent().trim());

        CapsuleContribution contribution = existing != null ? existing : new CapsuleContribution();
        if (existing == null) {
            contribution.setCapsule(capsule);
            contribution.setContributor(userRepository.getReferenceById(currentUserId));
        }
        contribution.setTitle(trimToNull(form.getTitle()));
        contribution.setMessageContent(encrypted.ciphertextBase64());
        contribution.setEncryptionKeyVersion(encrypted.keyVersion());
        contribution.setEncryptionIv(encrypted.iv());
        contribution.setStatus(ContributionStatus.SUBMITTED);
        contributionRepository.save(contribution);

        auditService.record(
                currentUserId,
                Role.ROLE_USER,
                existing == null ? "CONTRIBUTION_SUBMITTED" : "CONTRIBUTION_UPDATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("contributionPublicId", contribution.getPublicId())
        );
    }

    @Transactional
    public void toggleContributorEditPermission(
            String publicId,
            Long memberId,
            Long ownerUserId,
            String ip,
            String userAgent
    ) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwnerOfSharedDraft(capsule, ownerUserId);

        CapsuleMember member = capsuleMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        if (!member.getCapsule().getId().equals(capsule.getId()) || member.getMemberRole() == MemberRole.OWNER) {
            throw new BusinessException("Cannot modify edit permissions for this member.");
        }

        member.setCanEditBeforeSeal(!member.isCanEditBeforeSeal());
        capsuleMemberRepository.save(member);

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CONTRIBUTOR_PERMISSION_UPDATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("memberId", member.getId(), "canEditBeforeSeal", member.isCanEditBeforeSeal())
        );
    }

    @Transactional
    public void removeContributor(
            String publicId,
            Long memberId,
            Long ownerUserId,
            String ip,
            String userAgent
    ) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwnerOfSharedDraft(capsule, ownerUserId);

        CapsuleMember member = capsuleMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        if (!member.getCapsule().getId().equals(capsule.getId()) || member.getMemberRole() == MemberRole.OWNER) {
            throw new BusinessException("Cannot remove this member.");
        }

        member.setInvitationStatus(InvitationStatus.REMOVED);
        capsuleMemberRepository.save(member);

        Long removedUserId = member.getUser().getId();
        contributionRepository.findFirstByCapsuleIdAndContributorIdOrderByIdAsc(capsule.getId(), removedUserId)
                .ifPresent(contrib -> {
                    contrib.setStatus(ContributionStatus.WITHDRAWN);
                    contributionRepository.save(contrib);
                    visibilityRepository.deleteByContributionId(contrib.getId());
                });
        visibilityRepository.deleteByCapsuleIdAndAllowedUserId(capsule.getId(), removedUserId);

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CONTRIBUTOR_REMOVED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("removedUserId", removedUserId)
        );
    }

    @Transactional
    public void updateContributionVisibilityAllowList(
            String publicId,
            String contributionPublicId,
            List<Long> allowedUserIds,
            Long ownerUserId,
            String ip,
            String userAgent
    ) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwnerOfSharedDraft(capsule, ownerUserId);

        if (capsule.getVisibilityPolicy() != VisibilityPolicy.OWNER_DEFINED) {
            throw new BusinessException("Switch visibility policy to OWNER_DEFINED to configure custom rules.");
        }

        CapsuleContribution contribution = contributionRepository
                .findByPublicIdAndCapsuleIdWithContributor(contributionPublicId, capsule.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found."));

        // Filter requested user IDs strictly to currently ACCEPTED members of this capsule
        Set<Long> acceptedMemberIds = new HashSet<>();
        for (CapsuleMember member : capsuleMemberRepository.findByCapsuleIdAndStatusWithUser(
                capsule.getId(), InvitationStatus.ACCEPTED)) {
            acceptedMemberIds.add(member.getUser().getId());
        }

        visibilityRepository.deleteByContributionId(contribution.getId());

        User ownerRef = userRepository.getReferenceById(ownerUserId);
        Set<Long> grantedIds = new HashSet<>();
        if (allowedUserIds != null) {
            for (Long candidateUserId : allowedUserIds) {
                if (acceptedMemberIds.contains(candidateUserId) && grantedIds.add(candidateUserId)) {
                    CapsuleContributionVisibility rule = new CapsuleContributionVisibility();
                    rule.setId(new CapsuleContributionVisibilityId(contribution.getId(), candidateUserId));
                    rule.setContribution(contribution);
                    rule.setAllowedUser(userRepository.getReferenceById(candidateUserId));
                    rule.setGrantedBy(ownerRef);
                    visibilityRepository.save(rule);
                }
            }
        }

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CONTRIBUTION_VISIBILITY_UPDATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("contributionPublicId", contributionPublicId, "allowedCount", grantedIds.size())
        );
    }

    private void validateInvitationEligibility(CapsuleInvitation invitation, String currentUserEmail) {
        if (!invitation.getInviteeEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new AccessDeniedException(
                    "This invitation was sent to " + invitation.getInviteeEmail()
                            + ". Please log in with that verified account to respond."
            );
        }
        if (invitation.getStatus() != CapsuleInvitationStatus.PENDING) {
            throw new BusinessException("This invitation is no longer pending (status: " + invitation.getStatus() + ").");
        }
        if (!invitation.getExpiresAtUtc().isAfter(Instant.now())) {
            throw new BusinessException("This invitation has expired.");
        }
        if (invitation.getCapsule().getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("This capsule has already been sealed and is no longer accepting contributors.");
        }
    }

    private void assertOwnerOfSharedDraft(Capsule capsule, Long userId) {
        if (!capsule.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only the capsule owner can perform this action.");
        }
        if (capsule.getCapsuleType() != CapsuleType.SHARED) {
            throw new BusinessException("This action only applies to shared capsules.");
        }
        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("Changes are locked because this shared capsule is no longer in DRAFT.");
        }
    }

    private ZoneId parseZoneId(String timezone) {
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ex) {
            throw new BusinessException("Invalid timezone selected.");
        }
    }

    private Instant parseFutureUnlockInstant(String localDateTimeStr, ZoneId zoneId) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr.trim());
            Instant unlockAtUtc = ZonedDateTime.of(localDateTime, zoneId).toInstant();
            if (!unlockAtUtc.isAfter(Instant.now())) {
                throw new BusinessException("Unlock date and time must be in the future.");
            }
            return unlockAtUtc;
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Invalid unlock date/time format.");
        }
    }

    private String resolveUserTimezone(Long userId) {
        return userProfileRepository.findById(userId)
                .map(UserProfile::getDefaultTimezone)
                .filter(tz -> tz != null && !tz.isBlank())
                .orElse("UTC");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "CELEBRATION";
        }
        return theme.trim().toUpperCase(Locale.ROOT);
    }

}