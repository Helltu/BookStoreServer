package com.bsuir.book_store.orders.api;

import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.api.dto.UpdateOrderStatusRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    public ResponseEntity<Page<Order>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @Operation(summary = "Обновить статус заказа", description = "Обновляет статус выбранного заказа на предоставленный")
    @PatchMapping("/{id}/status")
    @IsManager
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        orderService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Детали заказа", description = "Возвращает заказ со списком товаров и адресом доставки")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Operation(summary = "Отменить заказ (Клиент)", description = "Отмена заказа пользователем (только в статусе NEW)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        orderService.cancelOrder(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}