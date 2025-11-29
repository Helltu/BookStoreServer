package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.orders.application.OrderService;
import com.bsuir.book_store.orders.domain.Order;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderAiTools {

    private final OrderService orderService;

    @Tool("Посмотреть последние заказы текущего пользователя.")
    public String getMyLastOrders(String username) {
        try {
            List<Order> orders = orderService.getMyOrders(username);
            if (orders.isEmpty()) {
                return "У вас пока нет заказов.";
            }

            return orders.stream()
                    .limit(3)
                    .map(o -> String.format("Заказ %s от %s. Статус: %s. Сумма: %s BYN",
                            o.getOrderNumber(),
                            o.getOrderDate(),
                            o.getStatus(),
                            o.getTotalCost()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Не удалось получить список заказов. Возможно, вы не авторизованы.";
        }
    }
}