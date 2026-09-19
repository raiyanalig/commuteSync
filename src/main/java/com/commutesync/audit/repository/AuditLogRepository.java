package com.commutesync.audit.repository;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.domain.AuditLog;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actor IS NULL OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :actor, '%')))
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            """)
    Page<AuditLog> search(@Param("action") AuditAction action,
                          @Param("actor") String actor,
                          @Param("entityType") String entityType,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);
}
