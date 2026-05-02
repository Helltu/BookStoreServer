package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.assistant.infrastructure.cache.UserProfileCache;
import com.bsuir.book_store.orders.application.OrderService;
import com.bsuir.book_store.orders.domain.Order;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderAiTools {

    private final OrderService orderService;
    private final UserProfileCache profileCache;

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Tool("Посмотреть последние заказы текущего пользователя.")
    public String getMyLastOrders() {
        try {
            List<Order> orders = profileCache.get(currentUsername()).getOrders();
            if (orders.isEmpty()) {
                return "У вас пока нет заказов.";
            }

            return orders.stream()
                    .limit(10)
                    .map(o -> {
                        String books = o.getOrderItems().stream()
                                .map(i -> i.getBookTitle())
                                .collect(Collectors.joining(", "));
                        return String.format("Заказ %s от %s. Статус: %s. Книги: %s. Сумма: %s BYN",
                                o.getOrderNumber(),
                                o.getOrderDate(),
                                o.getStatus(),
                                books,
                                o.getTotalCost());
                    })
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Не удалось получить список заказов. Возможно, вы не авторизованы.";
        }
    }

    @Tool("Получить детали конкретного заказа по его номеру (например, ORD-XXXXXXXX) или ID.")
    public String getOrderDetails(String orderIdentifier) {
        try {
            String username = currentUsername();
            Order order;
            try {
                order = orderService.getOrderById(UUID.fromString(orderIdentifier), username);
            } catch (IllegalArgumentException e) {
                List<Order> orders = orderService.getMyOrders(username);
                order = orders.stream()
                        .filter(o -> o.getOrderNumber().equalsIgnoreCase(orderIdentifier))
                        .findFirst()
                        .orElse(null);
            }

            if (order == null) {
                return "Заказ '" + orderIdentifier + "' не найден.";
            }

            String items = order.getOrderItems().stream()
                    .map(i -> String.format("  - %s x%d (%.2f BYN)", i.getBookTitle(), i.getQuantity(), i.getPricePerItem()))
                    .collect(Collectors.joining("\n"));

            return String.format("Заказ %s от %s\nСтатус: %s\nСостав:\n%s\nИтого: %s BYN",
                    order.getOrderNumber(),
                    order.getOrderDate(),
                    order.getStatus(),
                    items,
                    order.getTotalCost());
        } catch (Exception e) {
            return "Не удалось получить информацию о заказе: " + e.getMessage();
        }
    }
}