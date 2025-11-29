package com.bsuir.book_store.orders.api;

import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.application.OrderService;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<UUID> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.createOrder(request, userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername()));
    }

    @GetMapping
    @IsManager
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/{id}/status")
    @IsManager
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status
    ) {
        orderService.changeStatus(id, status);
        return ResponseEntity.ok().build();
    }
}