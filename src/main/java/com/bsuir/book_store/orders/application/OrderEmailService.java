package com.bsuir.book_store.orders.application;

import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOrderCreated(Order order) {
        String to = order.getUser().getEmail();
        String subject = "Заказ " + order.getOrderNumber() + " оформлен";
        String text = String.format(
                "Здравствуйте, %s!\n\nВаш заказ %s успешно оформлен.\nСумма: %s руб.\nДата доставки: %s\n\nСпасибо за покупку!",
                order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername(),
                order.getOrderNumber(),
                order.getTotalCost(),
                order.getDeliveryDetails() != null ? order.getDeliveryDetails().getDeliveryDate() : "уточняется"
        );
        send(to, subject, text);
    }

    @Async
    public void sendStatusChanged(Order order, OrderStatus newStatus) {
        String to = order.getUser().getEmail();
        String subject = "Статус заказа " + order.getOrderNumber() + " изменён";
        String text = String.format(
                "Здравствуйте, %s!\n\nСтатус вашего заказа %s изменён на: %s.",
                order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername(),
                order.getOrderNumber(),
                translateStatus(newStatus)
        );
        send(to, subject, text);
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String translateStatus(OrderStatus status) {
        return switch (status) {
            case NEW -> "Новый";
            case PROCESSING -> "В обработке";
            case SHIPPED -> "Отправлен";
            case DELIVERED -> "Доставлен";
            case CANCELLED -> "Отменён";
            case RETURN_REQUESTED -> "Запрошен возврат";
            case RETURNED -> "Возвращен";
        };
    }
}
