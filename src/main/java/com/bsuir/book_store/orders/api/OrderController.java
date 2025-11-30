package com.bsuir.book_store.orders.api;

import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.application.OrderService;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Управление заказами")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Оформить заказ", description = "Создает заказ на основе списка книг и данных о доставке")
    @PostMapping
    public ResponseEntity<UUID> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.createOrder(request, userDetails.getUsername()));
    }

    @Operation(summary = "Получить мои заказы", description = "Возвращает историю заказов текущего пользователя")
    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername()));
    }

    @Operation(summary = "Получить все заказы (Менеджер)", description = "Доступно только пользователям с ролью MANAGER")
    @GetMapping
    @IsManager
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @Operation(summary = "Обновить статус заказа", description = "Обновляет статус выбранного заказа на предоставленный")
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