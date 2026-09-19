package com.commutesync.audit.service;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.domain.AuditLog;
import com.commutesync.audit.dto.AuditLogResponse;
import com.commutesync.audit.repository.AuditLogRepository;
import com.commutesync.common.api.PageResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String SYSTEM_ACTOR = "system";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Audits run in their own transaction: they must persist even when the surrounding
     * business transaction is read-only (login) or later rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse record(AuditAction action, String actor, String entityType,
                                   Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setActor(actor);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("AUDIT action={} actor={} entity={}#{} details={}",
                action, actor, entityType, entityId, details);
        return AuditLogResponse.from(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse record(AuditAction action, String entityType, Long entityId, String details) {
        return record(action, currentActor(), entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditAction action, String actor, String entityType,
                                                 Instant from, Instant to, Pageable pageable) {
        String actorFilter = (actor == null || actor.isBlank()) ? null : actor.trim();
        String entityTypeFilter = (entityType == null || entityType.isBlank()) ? null : entityType.trim();
        Page<AuditLog> page = auditLogRepository.search(action, actorFilter, entityTypeFilter, from, to, pageable);
        return PageResponse.from(page.map(AuditLogResponse::from));
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_ACTOR;
        }
        return authentication.getName();
    }
}
