package com.bsuir.book_store.assistant.infrastructure.cache;

import com.bsuir.book_store.orders.application.OrderService;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.users.api.dto.WishlistBookDto;
import com.bsuir.book_store.users.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class UserProfileCache {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final UserService userService;
    private final OrderService orderService;

    private final ConcurrentHashMap<String, CachedProfile> cache = new ConcurrentHashMap<>();

    public Profile get(String username) {
        CachedProfile cached = cache.get(username);
        if (cached != null && Instant.now().isBefore(cached.getExpiresAt())) {
            return cached.getProfile();
        }
        Profile fresh = load(username);
        cache.put(username, new CachedProfile(fresh, Instant.now().plus(TTL)));
        return fresh;
    }

    public void invalidate(String username) {
        cache.remove(username);
    }

    private Profile load(String username) {
        List<WishlistBookDto> wishlist;
        List<Order> orders;
        try { wishlist = userService.getWishlist(username); } catch (Exception e) { wishlist = List.of(); }
        try { orders = orderService.getMyOrders(username); } catch (Exception e) { orders = List.of(); }

        Set<UUID> excluded = new HashSet<>();
        wishlist.forEach(b -> excluded.add(b.getId()));
        orders.forEach(o -> o.getOrderItems().forEach(i -> {
            if (i.getBookId() != null) excluded.add(i.getBookId());
        }));

        return new Profile(wishlist, orders, excluded);
    }

    @Value
    public static class Profile {
        List<WishlistBookDto> wishlist;
        List<Order> orders;
        Set<UUID> excludedBookIds;
    }

    @Value
    private static class CachedProfile {
        Profile profile;
        Instant expiresAt;
    }
}
