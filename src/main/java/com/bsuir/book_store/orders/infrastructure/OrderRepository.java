package com.bsuir.book_store.orders.infrastructure;

import com.bsuir.book_store.orders.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserUsernameOrderByCreatedAtDesc(String username);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
}