package com.timefold.capsule.service;

import com.timefold.audit.service.AuditService;
import com.timefold.capsule.entity.Capsule;
import com.timefold.capsule.event.CapsuleUnlockedEvent;
import com.timefold.capsule.repository.CapsuleRepository;
import com.timefold.common.enums.CapsuleStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapsuleUnlockService {

    private static final Logger log = LoggerFactory.getLogger(CapsuleUnlockService.class);
    private static final int BATCH_SIZE = 50;

    private final CapsuleRepository capsuleRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public CapsuleUnlockService(
            CapsuleRepository capsuleRepository,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.capsuleRepository = capsuleRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public int processDueCapsules(String triggerSource) {
        Instant now = Instant.now();
        int totalUnlocked = 0;

        while (true) {
            List<Long> dueIds = capsuleRepository.findDueSealedCapsuleIds(now, PageRequest.of(0, BATCH_SIZE));
            if (dueIds.isEmpty()) {
                break;
            }
            int batchUnlocked = 0;
            for (Long capsuleId : dueIds) {
                if (unlockSingleCapsule(capsuleId, now, triggerSource)) {
                    batchUnlocked++;
                    totalUnlocked++;
                }
            }
            if (batchUnlocked == 0) {
                break;
            }
        }

        if (totalUnlocked > 0) {
            log.info("Capsule unlock engine [{}] transitioned {} capsule(s) to READY.", triggerSource, totalUnlocked);
        }
        return totalUnlocked;
    }

    @Transactional
    public boolean unlockSingleCapsule(Long capsuleId, Instant now, String triggerSource) {
        int updated = capsuleRepository.unlockSealedCapsuleAtomically(capsuleId, now);
        if (updated != 1) {
            // Already unlocked or cancelled by another transaction — strictly idempotent
            return false;
        }

        Capsule capsule = capsuleRepository.findById(capsuleId).orElse(null);
        String publicId = capsule != null ? capsule.getPublicId() : String.valueOf(capsuleId);

        auditService.record(
                null,
                "SYSTEM",
                "CAPSULE_UNLOCKED",
                "CAPSULE",
                publicId,
                null,
                null,
                Map.of("trigger", triggerSource, "unlockedAtUtc", now.toString())
        );

        eventPublisher.publishEvent(new CapsuleUnlockedEvent(capsuleId, publicId, now));
        return true;
    }

    @Transactional
    public boolean tryLazyUnlockIfDue(Capsule capsule) {
        Instant now = Instant.now();
        if (capsule.getStatus() == CapsuleStatus.SEALED && !capsule.getUnlockAtUtc().isAfter(now)) {
            return unlockSingleCapsule(capsule.getId(), now, "LAZY_ACCESS");
        }
        return false;
    }

}