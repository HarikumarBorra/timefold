package com.timefold.audit.service;

import com.timefold.audit.entity.AuditLog;
import com.timefold.audit.repository.AuditLogRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(
            Long actorUserId,
            String actorRole,
            String eventType,
            String entityType,
            String entityId,
            String ipAddress,
            String userAgent,
            Map<String, Object> metadata
    ) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setActorRole(actorRole);
        log.setEventType(eventType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setIpAddress(truncate(ipAddress, 45));
        log.setUserAgent(truncate(userAgent, 255));
        log.setMetadataJson(metadata);
        auditLogRepository.save(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

}