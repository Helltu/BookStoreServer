package com.bsuir.book_store.orders.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateDeliverySlotRequest {
    @NotBlank
    private String timeSlot;

    @NotNull
    private LocalDate deliveryDate;
}
