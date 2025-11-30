package com.bsuir.book_store.users.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_addresses")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "address_text", nullable = false)
    private String addressText;

    @Column(name = "is_default")
    private Boolean isDefault;

    void assignToUser(User user) {
        this.user = user;
    }

    public void makeDefault() {
        this.isDefault = true;
    }

    void unsetDefault() {
        this.isDefault = false;
    }
}