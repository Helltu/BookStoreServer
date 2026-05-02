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

        StringBuilder items = new StringBuilder();
        order.getOrderItems().forEach(item -> items
                .append("  • ").append(item.getBookTitle())
                .append(" x").append(item.getQuantity())
                .append(" — ").append(item.getPricePerItem().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))).append(" руб.\n")
        );

        String text = String.format(
                "Здравствуйте, %s!\n\n" +
                "Ваш заказ %s успешно оформлен.\n\n" +
                "Состав заказа:\n%s\n" +
                "Итого: %s руб.\n\n" +
                "Наш менеджер свяжется с вами в ближайшее время для подтверждения заказа и уточнения деталей доставки.\n\n" +
                "Спасибо за покупку!",
                order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername(),
                order.getOrderNumber(),
                items,
                order.getTotalCost()
        );
        send(to, subject, text);
    }

    @Async
    public void sendStatusChanged(Order order, OrderStatus newStatus) {
        String to = order.getUser().getEmail();
        String subject = "Статус заказа " + order.getOrderNumber() + " изменён";
        String extra = newStatus == OrderStatus.DELIVERED
                ? "\n\nНадеемся, вам понравились книги! Не забудьте оставить отзыв — это поможет другим читателям сделать правильный выбор."
                : "";
        String text = String.format(
                "Здравствуйте, %s!\n\nСтатус вашего заказа %s изменён на: %s.%s",
                order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername(),
                order.getOrderNumber(),
                translateStatus(newStatus),
                extra
        );
        send(to, subject, text);
    }

    @Async
    public void sendDeliverySlotAssigned(Order order) {
        String to = order.getUser().getEmail();
        String subject = "Дата доставки заказа " + order.getOrderNumber() + " назначена";
        var details = order.getDeliveryDetails();
        String formattedDate = details.getDeliveryDate() != null
                ? details.getDeliveryDate().toLocalDate().format(
                    java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("ru")))
                : "не указана";
        String text = String.format(
                "Здравствуйте, %s!\n\n" +
                "По вашему заказу %s назначена доставка.\n\n" +
                "Дата доставки: %s\n" +
                "Временной слот: %s\n\n" +
                "Адрес доставки: %s",
                order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername(),
                order.getOrderNumber(),
                formattedDate,
                details.getDeliveryTimeSlot() != null ? details.getDeliveryTimeSlot() : "не указан",
                details.getAddressText()
        );
        send(to, subject, text);
    }

    @Async
    public void sendPasswordChanged(String email, String username) {
        String subject = "Пароль изменён";
        String text = String.format(
                "Здравствуйте, %s!\n\nПароль от вашего аккаунта был успешно изменён.\n\nЕсли это были не вы — немедленно свяжитесь с поддержкой.",
                username
        );
        send(email, subject, text);
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
    }it

    private String translateStatus(OrderStatus status) {
        return switch (status) {
            case NEW -> "Новый";
            case PROCESSING -> "В обработке";
            case SHIPPED -> "Отправлен";
            case DELIVERED -> "Доставлен";
            case CANCELLED -> "Отменён";
            case RETURN_REQUESTED -> "Запрошен возврат";
            case RETURNED -> "Возвращен";
            case FAILED -> "Доставка не удалась";
        };
    }
}
