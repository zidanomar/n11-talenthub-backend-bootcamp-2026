package com.example.order.repository;

import com.example.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
    Page<Order> findByUserId(String userId, Pageable pageable);

    @Query(value = """
            select o from Order o
            where o.userId = :userId
              and (cast(:fromDate as LocalDateTime) is null or o.createdAt >= :fromDate)
              and (cast(:toDate as LocalDateTime) is null or o.createdAt < :toDate)
            """,
           countQuery = """
            select count(o) from Order o
            where o.userId = :userId
              and (cast(:fromDate as LocalDateTime) is null or o.createdAt >= :fromDate)
              and (cast(:toDate as LocalDateTime) is null or o.createdAt < :toDate)
            """)
    Page<Order> findByUserIdAndCreatedAtRange(
            @Param("userId") String userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query(value = """
            select o from Order o
            where (cast(:fromDate as LocalDateTime) is null or o.createdAt >= :fromDate)
              and (cast(:toDate as LocalDateTime) is null or o.createdAt < :toDate)
            """,
           countQuery = """
            select count(o) from Order o
            where (cast(:fromDate as LocalDateTime) is null or o.createdAt >= :fromDate)
              and (cast(:toDate as LocalDateTime) is null or o.createdAt < :toDate)
            """)
    Page<Order> findByCreatedAtRange(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
