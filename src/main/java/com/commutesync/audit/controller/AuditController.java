package com.commutesync.audit.controller;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.dto.AuditLogResponse;
import com.commutesync.audit.service.AuditService;
import com.commutesync.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit", description = "Audit trail (ADMIN only)")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Search the audit trail with optional filters")
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return auditService.search(action, actor, entityType, from, to, pageable);
    }
}
