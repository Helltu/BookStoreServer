package com.bsuir.book_store.orders.infrastructure;

import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserUsernameOrderByCreatedAtDesc(String username);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN o.deliveryDetails d WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:orderNumber IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) AND " +
            "(:customerName IS NULL OR LOWER(d.customerName) LIKE LOWER(CONCAT('%', :customerName, '%'))) AND " +
            "(:from IS NULL OR o.createdAt >= :from) AND " +
            "(:to IS NULL OR o.createdAt <= :to)")
    Page<Order> findWithFilters(
            @Param("status") OrderStatus status,
            @Param("orderNumber") String orderNumber,
            @Param("customerName") String customerName,
            @Param("from") Timestamp from,
            @Param("to") Timestamp to,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o LEFT JOIN o.deliveryDetails d WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:orderNumber IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) AND " +
            "(:customerName IS NULL OR LOWER(d.customerName) LIKE LOWER(CONCAT('%', :customerName, '%'))) AND " +
            "(:from IS NULL OR o.createdAt >= :from) AND " +
            "(:to IS NULL OR o.createdAt <= :to)")
    List<Order> findAllWithFilters(
            @Param("status") OrderStatus status,
            @Param("orderNumber") String orderNumber,
            @Param("customerName") String customerName,
            @Param("from") Timestamp from,
            @Param("to") Timestamp to
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalCost) FROM Order o WHERE o.status = :status")
    java.math.BigDecimal sumTotalCostByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(DISTINCT o.user) FROM Order o WHERE o.status NOT IN :excludedStatuses AND (:from IS NULL OR o.createdAt >= :from) AND (:to IS NULL OR o.createdAt <= :to)")
    long countUniqueCustomers(@Param("excludedStatuses") List<OrderStatus> excludedStatuses, @Param("from") java.sql.Timestamp from, @Param("to") java.sql.Timestamp to);

    @Query("SELECT o.user.username, o.user.firstName, o.user.lastName, COUNT(o), SUM(o.totalCost) FROM Order o WHERE o.status NOT IN :excludedStatuses AND (:from IS NULL OR o.createdAt >= :from) AND (:to IS NULL OR o.createdAt <= :to) GROUP BY o.user.username, o.user.firstName, o.user.lastName ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> findTopCustomers(@Param("excludedStatuses") List<OrderStatus> excludedStatuses, @Param("from") java.sql.Timestamp from, @Param("to") java.sql.Timestamp to, Pageable pageable);

    @Query("SELECT AVG(CAST(FUNCTION('TIMESTAMPDIFF', HOUR, o.createdAt, o.updatedAt) AS double)) FROM Order o WHERE o.status = :status AND (:from IS NULL OR o.createdAt >= :from) AND (:to IS NULL OR o.createdAt <= :to)")
    Double avgDeliveryHours(@Param("status") OrderStatus status, @Param("from") java.sql.Timestamp from, @Param("to") java.sql.Timestamp to);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o JOIN o.orderItems oi WHERE o.user.username = :username AND oi.bookId = :bookId AND o.status IN (com.bsuir.book_store.orders.domain.OrderStatus.DELIVERED, com.bsuir.book_store.orders.domain.OrderStatus.RETURNED)")
    boolean existsDeliveredOrderForUserAndBook(@Param("username") String username, @Param("bookId") UUID bookId);
}