package com.commutesync.notification.repository;

import com.commutesync.notification.domain.Notification;
import com.commutesync.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    Page<Notification> findByRecipientEmailAndStatusOrderByCreatedAtDesc(
            String recipientEmail, NotificationStatus status, Pageable pageable);

    long countByRecipientEmailAndStatus(String recipientEmail, NotificationStatus status);

    Optional<Notification> findByIdAndRecipientEmail(Long id, String recipientEmail);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.status = com.commutesync.notification.domain.NotificationStatus.READ, n.readAt = :readAt
            WHERE n.recipientEmail = :email AND n.status = com.commutesync.notification.domain.NotificationStatus.UNREAD
            """)
    int markAllRead(@Param("email") String email, @Param("readAt") Instant readAt);
}
