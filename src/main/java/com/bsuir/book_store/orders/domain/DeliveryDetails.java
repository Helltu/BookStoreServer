package com.bsuir.book_store.orders.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.util.UUID;

@Entity
@Table(name = "delivery_details")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDetails {
    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @JsonIgnore
    @OneToOne
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "address_text", columnDefinition = "TEXT", nullable = false)
    private String addressText;

    @Column(name = "delivery_time_slot")
    private String deliveryTimeSlot;

    @Column(name = "delivery_date")
    private Date deliveryDate;
}