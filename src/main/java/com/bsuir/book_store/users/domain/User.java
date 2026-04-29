package com.bsuir.book_store.users.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User implements UserDetails {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._]{4,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAddress> addresses = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "user_wishlist",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> wishlist = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp updatedAt;

    public static User register(String username, String email, String encodedPassword, Role role,
                                String firstName, String lastName, String phone) {
        validateEmail(email);
        validateUsername(username);

        User user = new User();
        user.username = username;
        user.email = email;
        user.password = encodedPassword;
        user.role = role != null ? role : Role.CLIENT;
        user.createdAt = Timestamp.valueOf(LocalDateTime.now());

        user.updateProfile(firstName, lastName, phone);

        return user;
    }

    public void updateProfile(String firstName, String lastName, String contactPhone) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName.trim();
        }
        if (contactPhone != null && !contactPhone.isBlank()) {
            validatePhone(contactPhone);
            this.contactPhone = contactPhone.trim();
        }
    }

    public void addAddress(UserAddress address) {
        if (address == null) throw new DomainException("Адрес не может быть пустым");

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            this.addresses.forEach(UserAddress::unsetDefault);
        } else if (this.addresses.isEmpty()) {
            address.makeDefault();
        }

        address.assignToUser(this);
        this.addresses.add(address);
    }

    public void removeAddress(UUID addressId) {
        boolean removed = this.addresses.removeIf(a -> a.getId().equals(addressId));
        if (!removed) {
            throw new DomainException("Адрес не найден");
        }
    }

    public void updateAddress(UUID addressId, String addressName, String addressText, Boolean isDefault) {
        UserAddress address = this.addresses.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Адрес не найден"));

        if (Boolean.TRUE.equals(isDefault)) {
            this.addresses.forEach(UserAddress::unsetDefault);
        }
        address.update(addressName, addressText, isDefault);
    }

    public void addToWishlist(Book book) {
        if (book != null) {
            this.wishlist.add(book);
        }
    }

    public void removeFromWishlist(Book book) {
        if (book != null) {
            this.wishlist.remove(book);
        }
    }

    public void changePassword(String newEncodedPassword) {
        if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
            throw new DomainException("Хэш пароля не может быть пустым");
        }
        this.password = newEncodedPassword;
    }

    public static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new DomainException("Пароль должен содержать минимум 8 символов, включая буквы и цифры");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new DomainException("Некорректный формат email");
        }
    }

    private static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new DomainException("Логин должен содержать от 4 до 20 символов (латиница, цифры, '.', '_')");
        }
    }

    private static void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new DomainException("Некорректный формат телефона. Пример: +375291234567");
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}