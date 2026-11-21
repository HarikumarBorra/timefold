package com.timefold.capsule.service;

import com.timefold.audit.service.AuditService;
import com.timefold.capsule.dto.CapsuleCardDto;
import com.timefold.capsule.dto.CapsuleDetailView;
import com.timefold.capsule.dto.PersonalCapsuleForm;
import com.timefold.capsule.entity.Capsule;
import com.timefold.capsule.entity.CapsuleMember;
import com.timefold.capsule.entity.CapsuleOpen;
import com.timefold.capsule.entity.CapsuleOpenId;
import com.timefold.capsule.repository.CapsuleMemberRepository;
import com.timefold.capsule.repository.CapsuleOpenRepository;
import com.timefold.capsule.repository.CapsuleRepository;
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
import com.timefold.contribution.entity.CapsuleContribution;
import com.timefold.contribution.entity.CapsuleContributionVisibility;
import com.timefold.contribution.repository.CapsuleContributionRepository;
import com.timefold.contribution.repository.CapsuleContributionVisibilityRepository;
import com.timefold.invitation.repository.CapsuleInvitationRepository;
import com.timefold.user.dto.UserDashboardSummary;
import com.timefold.user.entity.Role;
import com.timefold.user.entity.User;
import com.timefold.user.entity.UserProfile;
import com.timefold.user.repository.UserProfileRepository;
import com.timefold.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalCapsuleService {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a z");
    private static final DateTimeFormatter INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final CapsuleRepository capsuleRepository;
    private final CapsuleMemberRepository capsuleMemberRepository;
    private final CapsuleContributionRepository contributionRepository;
    private final CapsuleContributionVisibilityRepository visibilityRepository;
    private final CapsuleInvitationRepository invitationRepository;
    private final CapsuleOpenRepository capsuleOpenRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MessageEncryptionService encryptionService;
    private final CapsuleUnlockService capsuleUnlockService;
    private final AuditService auditService;

    public PersonalCapsuleService(
            CapsuleRepository capsuleRepository,
            CapsuleMemberRepository capsuleMemberRepository,
            CapsuleContributionRepository contributionRepository,
            CapsuleContributionVisibilityRepository visibilityRepository,
            CapsuleInvitationRepository invitationRepository,
            CapsuleOpenRepository capsuleOpenRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            MessageEncryptionService encryptionService,
            CapsuleUnlockService capsuleUnlockService,
            AuditService auditService
    ) {
        this.capsuleRepository = capsuleRepository;
        this.capsuleMemberRepository = capsuleMemberRepository;
        this.contributionRepository = contributionRepository;
        this.visibilityRepository = visibilityRepository;
        this.invitationRepository = invitationRepository;
        this.capsuleOpenRepository = capsuleOpenRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.encryptionService = encryptionService;
        this.capsuleUnlockService = capsuleUnlockService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PersonalCapsuleForm prepareNewForm(Long userId) {
        String userTz = resolveUserTimezone(userId);
        ZoneId zoneId = ZoneId.of(userTz);
        ZonedDateTime defaultUnlock = ZonedDateTime.now(zoneId).plusDays(7).withSecond(0).withNano(0);

        PersonalCapsuleForm form = new PersonalCapsuleForm();
        form.setTimezone(userTz);
        form.setUnlockLocalDateTime(INPUT_FORMATTER.format(defaultUnlock));
        form.setTheme("CLASSIC");
        return form;
    }

    @Transactional
    public String createPersonalDraft(Long ownerUserId, PersonalCapsuleForm form, String ip, String userAgent) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        ZoneId zoneId = parseZoneId(form.getTimezone());
        Instant unlockAtUtc = parseFutureUnlockInstant(form.getUnlockLocalDateTime(), zoneId);

        Capsule capsule = new Capsule();
        capsule.setOwner(owner);
        capsule.setTitle(form.getTitle().trim());
        capsule.setDescription(trimToNull(form.getDescription()));
        capsule.setTheme(normalizeTheme(form.getTheme()));
        capsule.setCapsuleType(CapsuleType.PERSONAL);
        capsule.setVisibilityPolicy(VisibilityPolicy.SELF_ONLY);
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

        MessageEncryptionService.EncryptedPayload encrypted = encryptionService.encrypt(form.getMessageContent());
        CapsuleContribution contribution = new CapsuleContribution();
        contribution.setCapsule(capsule);
        contribution.setContributor(owner);
        contribution.setTitle(capsule.getTitle());
        contribution.setMessageContent(encrypted.ciphertextBase64());
        contribution.setEncryptionKeyVersion(encrypted.keyVersion());
        contribution.setEncryptionIv(encrypted.iv());
        contribution.setStatus(ContributionStatus.SUBMITTED);
        contributionRepository.save(contribution);

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CAPSULE_CREATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("type", "PERSONAL", "unlockAtUtc", unlockAtUtc.toString())
        );

        return capsule.getPublicId();
    }

    @Transactional(readOnly = true)
    public PersonalCapsuleForm loadDraftForEdit(String publicId, Long ownerUserId) {
        Capsule capsule = capsuleRepository.findByPublicIdWithOwner(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwner(capsule, ownerUserId);

        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("Only DRAFT capsules can be edited.");
        }

        CapsuleContribution contribution = contributionRepository
                .findFirstByCapsuleIdAndContributorIdOrderByIdAsc(capsule.getId(), ownerUserId)
                .orElse(null);

        String decrypted = contribution != null
                ? encryptionService.decrypt(
                        contribution.getMessageContent(),
                        contribution.getEncryptionIv(),
                        contribution.getEncryptionKeyVersion())
                : "";

        ZoneId zoneId = ZoneId.of(capsule.getTimezone());
        PersonalCapsuleForm form = new PersonalCapsuleForm();
        form.setTitle(capsule.getTitle());
        form.setDescription(capsule.getDescription());
        form.setTheme(capsule.getTheme() != null ? capsule.getTheme() : "CLASSIC");
        form.setMessageContent(decrypted);
        form.setTimezone(capsule.getTimezone());
        form.setUnlockLocalDateTime(INPUT_FORMATTER.format(capsule.getUnlockAtUtc().atZone(zoneId)));
        return form;
    }

    @Transactional
    public void updatePersonalDraft(String publicId, Long ownerUserId, PersonalCapsuleForm form, String ip, String userAgent) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwner(capsule, ownerUserId);

        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("This capsule has already been sealed and can no longer be edited.");
        }

        ZoneId zoneId = parseZoneId(form.getTimezone());
        Instant unlockAtUtc = parseFutureUnlockInstant(form.getUnlockLocalDateTime(), zoneId);

        capsule.setTitle(form.getTitle().trim());
        capsule.setDescription(trimToNull(form.getDescription()));
        capsule.setTheme(normalizeTheme(form.getTheme()));
        capsule.setUnlockAtUtc(unlockAtUtc);
        capsule.setTimezone(zoneId.getId());
        capsuleRepository.save(capsule);

        if (capsule.getCapsuleType() == CapsuleType.PERSONAL) {
            MessageEncryptionService.EncryptedPayload encrypted = encryptionService.encrypt(form.getMessageContent());
            CapsuleContribution contribution = contributionRepository
                    .findFirstByCapsuleIdAndContributorIdOrderByIdAsc(capsule.getId(), ownerUserId)
                    .orElseGet(() -> {
                        CapsuleContribution fresh = new CapsuleContribution();
                        fresh.setCapsule(capsule);
                        fresh.setContributor(capsule.getOwner());
                        fresh.setStatus(ContributionStatus.SUBMITTED);
                        return fresh;
                    });

            contribution.setTitle(capsule.getTitle());
            contribution.setMessageContent(encrypted.ciphertextBase64());
            contribution.setEncryptionKeyVersion(encrypted.keyVersion());
            contribution.setEncryptionIv(encrypted.iv());
            contributionRepository.save(contribution);
        }

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CAPSULE_UPDATED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("unlockAtUtc", unlockAtUtc.toString())
        );
    }

    @Transactional
    public void sealCapsule(String publicId, Long ownerUserId, String ip, String userAgent) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwner(capsule, ownerUserId);

        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("Only DRAFT capsules can be sealed.");
        }

        Instant now = Instant.now();
        if (!capsule.getUnlockAtUtc().isAfter(now)) {
            throw new BusinessException("Unlock date and time must still be in the future before sealing.");
        }

        long submittedCount = contributionRepository.countByCapsuleIdAndStatus(
                capsule.getId(), ContributionStatus.SUBMITTED);
        if (submittedCount == 0) {
            throw new BusinessException("Cannot seal an empty capsule. At least one submitted message is required.");
        }

        capsule.setStatus(CapsuleStatus.SEALED);
        capsule.setSealedAtUtc(now);
        capsuleRepository.save(capsule);

        // Revoke any unaccepted contributor invitations so nobody joins an already-sealed capsule
        invitationRepository.revokeAllPendingForCapsule(capsule.getId(), now);

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CAPSULE_SEALED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("unlockAtUtc", capsule.getUnlockAtUtc().toString(), "submittedCount", submittedCount)
        );
    }

    @Transactional
    public void cancelSealedCapsule(String publicId, Long ownerUserId, String ip, String userAgent) {
        Capsule capsule = capsuleRepository.findByPublicIdWithOwner(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwner(capsule, ownerUserId);

        int updated = capsuleRepository.cancelSealedCapsuleAtomically(capsule.getId(), ownerUserId, Instant.now());
        if (updated != 1) {
            throw new BusinessException("Only SEALED capsules can be cancelled (this capsule may have already unlocked).");
        }

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CAPSULE_CANCELLED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of()
        );
    }

    @Transactional
    public void deleteDraftCapsule(String publicId, Long ownerUserId, String ip, String userAgent) {
        Capsule capsule = capsuleRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertOwner(capsule, ownerUserId);

        if (capsule.getStatus() != CapsuleStatus.DRAFT) {
            throw new BusinessException("Only DRAFT capsules can be permanently deleted.");
        }

        capsuleRepository.delete(capsule);

        auditService.record(
                ownerUserId,
                Role.ROLE_USER,
                "CAPSULE_DRAFT_DELETED",
                "CAPSULE",
                publicId,
                ip,
                userAgent,
                Map.of()
        );
    }

    @Transactional
    public void openCapsule(String publicId, Long viewerUserId, String ip, String userAgent) {
        Capsule capsule = capsuleRepository.findByPublicIdWithOwner(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        assertAuthorizedMember(capsule, viewerUserId);

        Instant now = Instant.now();
        if (capsule.getStatus() == CapsuleStatus.SEALED) {
            if (capsule.getUnlockAtUtc().isAfter(now)) {
                throw new BusinessException("This capsule is still sealed until its scheduled unlock time.");
            }
            capsuleUnlockService.tryLazyUnlockIfDue(capsule);
            capsule = capsuleRepository.findByPublicIdWithOwner(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        }

        if (capsule.getStatus() != CapsuleStatus.READY && capsule.getStatus() != CapsuleStatus.OPENED) {
            throw new BusinessException("This capsule cannot be opened in its current state.");
        }

        capsuleRepository.markReadyCapsuleOpenedAtomically(capsule.getId(), now);

        User viewer = userRepository.getReferenceById(viewerUserId);
        CapsuleOpen openRecord = capsuleOpenRepository.findByCapsuleIdAndUserId(capsule.getId(), viewerUserId)
                .orElse(null);

        if (openRecord == null) {
            openRecord = new CapsuleOpen();
            openRecord.setId(new CapsuleOpenId(capsule.getId(), viewerUserId));
            openRecord.setCapsule(capsule);
            openRecord.setUser(viewer);
            openRecord.setFirstOpenedAtUtc(now);
            openRecord.setLastOpenedAtUtc(now);
            openRecord.setOpenCount(1);
        } else {
            openRecord.setLastOpenedAtUtc(now);
            openRecord.setOpenCount(openRecord.getOpenCount() + 1);
        }
        capsuleOpenRepository.save(openRecord);

        auditService.record(
                viewerUserId,
                Role.ROLE_USER,
                "CAPSULE_OPENED",
                "CAPSULE",
                capsule.getPublicId(),
                ip,
                userAgent,
                Map.of("openCount", openRecord.getOpenCount())
        );
    }

    @Transactional
    public CapsuleDetailView getCapsuleDetail(String publicId, Long viewerUserId) {
        Capsule capsule = capsuleRepository.findByPublicIdWithOwner(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        CapsuleMember currentMember = assertAuthorizedMember(capsule, viewerUserId);

        if (capsuleUnlockService.tryLazyUnlockIfDue(capsule)) {
            capsule = capsuleRepository.findByPublicIdWithOwner(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Capsule not found."));
        }

        boolean isOwner = capsule.getOwner().getId().equals(viewerUserId);
        boolean isShared = capsule.getCapsuleType() == CapsuleType.SHARED;
        ZoneId displayZone = ZoneId.of(capsule.getTimezone());
        CapsuleStatus status = capsule.getStatus();

        CapsuleOpen userOpen = capsuleOpenRepository.findByCapsuleIdAndUserId(capsule.getId(), viewerUserId)
                .orElse(null);
        boolean openedByCurrentUser = userOpen != null;

        boolean canEdit = isOwner && status == CapsuleStatus.DRAFT && !isShared;
        boolean canSeal = isOwner && status == CapsuleStatus.DRAFT;
        boolean canDeleteDraft = isOwner && status == CapsuleStatus.DRAFT;
        boolean canCancel = isOwner && status == CapsuleStatus.SEALED;
        boolean canOpenNow = (status == CapsuleStatus.READY || status == CapsuleStatus.OPENED) && !openedByCurrentUser;

        List<CapsuleContribution> submittedContributions = contributionRepository
                .findByCapsuleIdAndStatusWithContributor(capsule.getId(), ContributionStatus.SUBMITTED);

        // Current user's own contribution (decrypted only in DRAFT for editing)
        CapsuleContribution ownContribution = submittedContributions.stream()
                .filter(c -> c.getContributor().getId().equals(viewerUserId))
                .findFirst()
                .orElse(null);
        boolean hasOwnContribution = ownContribution != null;
        boolean canSubmitOwnContribution = isShared && status == CapsuleStatus.DRAFT
                && (!hasOwnContribution || isOwner || (currentMember != null && currentMember.isCanEditBeforeSeal()));

        String ownContributionTitle = null;
        String ownContributionMessage = null;
        if (isShared && status == CapsuleStatus.DRAFT && ownContribution != null) {
            ownContributionTitle = ownContribution.getTitle();
            ownContributionMessage = encryptionService.decrypt(
                    ownContribution.getMessageContent(),
                    ownContribution.getEncryptionIv(),
                    ownContribution.getEncryptionKeyVersion()
            );
        }

        // Personal message reveal
        boolean showPersonalMessage = !isShared && ((status == CapsuleStatus.DRAFT && isOwner)
                || ((status == CapsuleStatus.READY || status == CapsuleStatus.OPENED) && openedByCurrentUser));
        String decryptedPersonalMessage = null;
        if (showPersonalMessage && !submittedContributions.isEmpty()) {
            CapsuleContribution personal = submittedContributions.get(0);
            decryptedPersonalMessage = encryptionService.decrypt(
                    personal.getMessageContent(),
                    personal.getEncryptionIv(),
                    personal.getEncryptionKeyVersion()
            );
        }

        // Shared members & invitations
        List<CapsuleDetailView.MemberItem> memberItems = List.of();
        List<CapsuleDetailView.InvitationItem> invitationItems = List.of();
        List<CapsuleDetailView.DraftContributionSummary> draftSummaries = List.of();

        if (isShared) {
            Set<Long> submittedUserIds = new HashSet<>();
            for (CapsuleContribution c : submittedContributions) {
                submittedUserIds.add(c.getContributor().getId());
            }

            memberItems = capsuleMemberRepository
                    .findByCapsuleIdAndStatusWithUser(capsule.getId(), InvitationStatus.ACCEPTED).stream()
                    .map(m -> new CapsuleDetailView.MemberItem(
                            m.getId(),
                            m.getUser().getId(),
                            m.getUser().getFullName(),
                            m.getUser().getEmail(),
                            m.getMemberRole(),
                            m.isCanEditBeforeSeal(),
                            submittedUserIds.contains(m.getUser().getId()),
                            formatInstant(m.getJoinedAtUtc(), displayZone)
                    ))
                    .toList();

            if (isOwner) {
                invitationItems = invitationRepository.findByCapsuleIdOrderByCreatedAtDesc(capsule.getId()).stream()
                        .map(inv -> new CapsuleDetailView.InvitationItem(
                                inv.getId(),
                                inv.getInviteeEmail(),
                                inv.getStatus(),
                                formatInstant(inv.getExpiresAtUtc(), displayZone)
                        ))
                        .toList();

                if (status == CapsuleStatus.DRAFT && !submittedContributions.isEmpty()) {
                    Map<Long, Set<Long>> allowMap = loadVisibilityMap(submittedContributions);
                    draftSummaries = submittedContributions.stream()
                            .map(c -> new CapsuleDetailView.DraftContributionSummary(
                                    c.getPublicId(),
                                    c.getContributor().getId(),
                                    c.getContributor().getFullName(),
                                    c.getTitle(),
                                    formatInstant(c.getUpdatedAtUtc(), displayZone),
                                    allowMap.getOrDefault(c.getId(), Collections.emptySet())
                            ))
                            .toList();
                }
            }
        }

        // Shared revealed contributions — strictly filtered by VisibilityPolicy after user opens capsule
        List<CapsuleDetailView.RevealedContributionItem> visibleContributions = List.of();
        if (isShared && (status == CapsuleStatus.READY || status == CapsuleStatus.OPENED) && openedByCurrentUser) {
            Map<Long, Set<Long>> allowMap = (capsule.getVisibilityPolicy() == VisibilityPolicy.OWNER_DEFINED && !isOwner)
                    ? loadVisibilityMap(submittedContributions)
                    : Collections.emptyMap();

            visibleContributions = submittedContributions.stream()
                    .filter(c -> canUserReadContribution(capsule, c, viewerUserId, isOwner, allowMap))
                    .map(c -> new CapsuleDetailView.RevealedContributionItem(
                            c.getPublicId(),
                            c.getContributor().getFullName(),
                            c.getContributor().getId().equals(viewerUserId),
                            c.getTitle(),
                            encryptionService.decrypt(
                                    c.getMessageContent(),
                                    c.getEncryptionIv(),
                                    c.getEncryptionKeyVersion()),
                            formatInstant(c.getUpdatedAtUtc(), displayZone)
                    ))
                    .toList();
        }

        List<CapsuleDetailView.OpenHistoryItem> openHistory = List.of();
        if (status == CapsuleStatus.READY || status == CapsuleStatus.OPENED) {
            openHistory = capsuleOpenRepository.findByCapsuleIdWithUser(capsule.getId()).stream()
                    .map(co -> new CapsuleDetailView.OpenHistoryItem(
                            co.getUser().getFullName(),
                            formatInstant(co.getFirstOpenedAtUtc(), displayZone),
                            formatInstant(co.getLastOpenedAtUtc(), displayZone),
                            co.getOpenCount()
                    ))
                    .toList();
        }

        return new CapsuleDetailView(
                capsule.getPublicId(),
                capsule.getTitle(),
                capsule.getDescription(),
                capsule.getTheme(),
                capsule.getCapsuleType(),
                capsule.getVisibilityPolicy(),
                status,
                isOwner,
                isShared,
                openedByCurrentUser,
                canEdit,
                canSeal,
                canCancel,
                canDeleteDraft,
                canOpenNow,
                canSubmitOwnContribution,
                hasOwnContribution,
                ownContributionTitle,
                ownContributionMessage,
                submittedContributions.size(),
                showPersonalMessage,
                decryptedPersonalMessage,
                formatInstant(capsule.getUnlockAtUtc(), displayZone),
                capsule.getUnlockAtUtc().toString(),
                capsule.getTimezone(),
                formatInstant(capsule.getSealedAtUtc(), displayZone),
                formatInstant(capsule.getUnlockedAtUtc(), displayZone),
                formatInstant(capsule.getCancelledAtUtc(), displayZone),
                formatInstant(capsule.getCreatedAtUtc(), displayZone),
                memberItems,
                invitationItems,
                draftSummaries,
                visibleContributions,
                openHistory
        );
    }

    @Transactional(readOnly = true)
    public Page<CapsuleCardDto> listAccessibleCapsules(Long userId, String filter, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        String normalizedFilter = filter == null ? "ALL" : filter.trim().toUpperCase();

        Page<Capsule> capsulePage = switch (normalizedFilter) {
            case "PERSONAL" -> capsuleRepository.findAccessibleByUserAndType(userId, CapsuleType.PERSONAL, pageable);
            case "SHARED" -> capsuleRepository.findAccessibleByUserAndType(userId, CapsuleType.SHARED, pageable);
            case "DRAFT" -> capsuleRepository.findAccessibleByUserAndStatus(userId, CapsuleStatus.DRAFT, pageable);
            case "SEALED" -> capsuleRepository.findAccessibleByUserAndStatus(userId, CapsuleStatus.SEALED, pageable);
            case "READY" -> capsuleRepository.findReadyForUser(userId, pageable);
            case "OPENED" -> capsuleRepository.findOpenedForUser(userId, pageable);
            case "CANCELLED" -> capsuleRepository.findAccessibleByUserAndStatus(userId, CapsuleStatus.CANCELLED, pageable);
            default -> capsuleRepository.findAccessibleByUser(userId, pageable);
        };

        List<Long> ids = capsulePage.getContent().stream().map(Capsule::getId).toList();
        Set<Long> openedIds = ids.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(capsuleOpenRepository.findOpenedCapsuleIdsForUser(userId, ids));

        return capsulePage.map(c -> toCardDto(c, openedIds.contains(c.getId())));
    }

    @Transactional(readOnly = true)
    public UserDashboardSummary buildLiveDashboardSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);

        String displayName = (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank())
                ? profile.getDisplayName()
                : user.getFullName();
        String timezone = (profile != null && profile.getDefaultTimezone() != null)
                ? profile.getDefaultTimezone()
                : "UTC";

        long personalCount = capsuleRepository.countByOwnerIdAndCapsuleType(userId, CapsuleType.PERSONAL);
        long sharedCount = capsuleMemberRepository.countAcceptedSharedCapsulesForUser(userId);
        long readyCount = capsuleRepository.countReadyForUser(userId);
        long sealedCount = capsuleRepository.countAccessibleByUserAndStatus(userId, CapsuleStatus.SEALED);
        long openedCount = capsuleRepository.countOpenedForUser(userId);
        long pendingInvitationsCount = invitationRepository.countActivePendingForEmail(user.getEmail(), Instant.now());

        return new UserDashboardSummary(
                displayName,
                user.getEmail(),
                timezone,
                personalCount,
                sharedCount,
                0,
                readyCount,
                sealedCount,
                openedCount,
                pendingInvitationsCount
        );
    }

    private boolean canUserReadContribution(
            Capsule capsule,
            CapsuleContribution contribution,
            Long viewerUserId,
            boolean isOwner,
            Map<Long, Set<Long>> allowMap
    ) {
        // Owner and the contribution's own author can always read it after unlock
        if (isOwner || contribution.getContributor().getId().equals(viewerUserId)) {
            return true;
        }
        return switch (capsule.getVisibilityPolicy()) {
            case ALL_MEMBERS -> true;
            case SELF_ONLY -> false;
            case OWNER_DEFINED -> allowMap
                    .getOrDefault(contribution.getId(), Collections.emptySet())
                    .contains(viewerUserId);
        };
    }

    private Map<Long, Set<Long>> loadVisibilityMap(List<CapsuleContribution> contributions) {
        if (contributions.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = contributions.stream().map(CapsuleContribution::getId).toList();
        Map<Long, Set<Long>> result = new HashMap<>();
        for (CapsuleContributionVisibility rule : visibilityRepository.findByContributionIds(ids)) {
            result.computeIfAbsent(rule.getId().getContributionId(), k -> new HashSet<>())
                    .add(rule.getId().getAllowedUserId());
        }
        return result;
    }

    private CapsuleCardDto toCardDto(Capsule capsule, boolean openedByCurrentUser) {
        ZoneId zoneId = ZoneId.of(capsule.getTimezone());
        CapsuleStatus effectiveStatus = capsule.getStatus();
        if (effectiveStatus == CapsuleStatus.READY || effectiveStatus == CapsuleStatus.OPENED) {
            effectiveStatus = openedByCurrentUser ? CapsuleStatus.OPENED : CapsuleStatus.READY;
        }
        return new CapsuleCardDto(
                capsule.getPublicId(),
                capsule.getTitle(),
                capsule.getDescription(),
                capsule.getTheme(),
                capsule.getCapsuleType(),
                effectiveStatus,
                formatInstant(capsule.getUnlockAtUtc(), zoneId),
                capsule.getUnlockAtUtc().toString(),
                capsule.getTimezone(),
                openedByCurrentUser
        );
    }

    private void assertOwner(Capsule capsule, Long userId) {
        if (!capsule.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only the capsule owner can perform this action.");
        }
    }

    private CapsuleMember assertAuthorizedMember(Capsule capsule, Long userId) {
        CapsuleMember member = capsuleMemberRepository.findByCapsuleIdAndUserId(capsule.getId(), userId)
                .filter(m -> m.getInvitationStatus() == InvitationStatus.ACCEPTED)
                .orElse(null);

        if (capsule.getOwner().getId().equals(userId)) {
            return member;
        }
        if (member == null) {
            throw new AccessDeniedException("You do not have permission to access this capsule.");
        }
        return member;
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

    private String formatInstant(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return DISPLAY_FORMATTER.format(instant.atZone(zoneId));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "CLASSIC";
        }
        return theme.trim().toUpperCase();
    }

}