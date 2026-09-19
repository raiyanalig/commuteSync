package com.commutesync.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.domain.AuditLog;
import com.commutesync.audit.dto.AuditLogResponse;
import com.commutesync.audit.repository.AuditLogRepository;
import com.commutesync.common.api.PageResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordPersistsWithExplicitActor() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        AuditLogResponse response = auditService.record(
                AuditAction.VEHICLE_CREATED, "admin@example.com", "VEHICLE", 5L, "Created vehicle");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.action()).isEqualTo(AuditAction.VEHICLE_CREATED);
        assertThat(response.actor()).isEqualTo("admin@example.com");
        assertThat(response.entityType()).isEqualTo("VEHICLE");
        assertThat(response.entityId()).isEqualTo(5L);
    }

    @Test
    void recordResolvesActorFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null, List.of()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogResponse response = auditService.record(AuditAction.TRIP_ASSIGNED, "TRIP", 7L, "Assigned");

        assertThat(response.actor()).isEqualTo("admin@example.com");
    }

    @Test
    void recordFallsBackToSystemWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogResponse response = auditService.record(AuditAction.TRIP_STARTED, "TRIP", 7L, "Started");

        assertThat(response.actor()).isEqualTo("system");
    }

    @Test
    void searchAppliesFiltersAndTrimsActor() {
        when(auditLogRepository.search(
                eq(AuditAction.LOGIN), eq("admin"), eq("USER"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLog())));

        PageResponse<AuditLogResponse> response = auditService.search(
                AuditAction.LOGIN, "  admin  ", "USER", null, null, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).action()).isEqualTo(AuditAction.LOGIN);
    }

    private AuditLog auditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction(AuditAction.LOGIN);
        auditLog.setActor("admin@example.com");
        auditLog.setEntityType("USER");
        auditLog.setDetails("User logged in");
        return auditLog;
    }
}
