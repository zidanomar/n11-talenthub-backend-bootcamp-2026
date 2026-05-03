package com.example.notification.repository;

import com.example.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);

    @Query(value = """
            select n from Notification n
            where n.userId = :userId
              and (cast(:fromDate as LocalDateTime) is null or n.createdAt >= :fromDate)
              and (cast(:toDate as LocalDateTime) is null or n.createdAt < :toDate)
            """,
           countQuery = """
            select count(n) from Notification n
            where n.userId = :userId
              and (cast(:fromDate as LocalDateTime) is null or n.createdAt >= :fromDate)
              and (cast(:toDate as LocalDateTime) is null or n.createdAt < :toDate)
            """)
    Page<Notification> findByUserIdAndCreatedAtRange(
            @Param("userId") String userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
