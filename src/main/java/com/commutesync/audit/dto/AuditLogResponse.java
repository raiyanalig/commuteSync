package com.commutesync.audit.dto;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.domain.AuditLog;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        AuditAction action,
        String actor,
        String entityType,
        Long entityId,
        String details,
        Instant timestamp
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getActor(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
