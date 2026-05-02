package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.assistant.infrastructure.cache.UserProfileCache;
import com.bsuir.book_store.users.api.dto.WishlistBookDto;
import com.bsuir.book_store.users.application.UserService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WishlistAiTools {

    private final UserService userService;
    private final UserProfileCache profileCache;

    @Tool("Получить список желаемого (вишлист) текущего пользователя для персонализированных рекомендаций.")
    public String getUserWishlist() {
        try {
            String username = currentUsername();
            List<WishlistBookDto> wishlist = profileCache.get(username).getWishlist();
            if (wishlist.isEmpty()) {
                return "Список желаемого пуст.";
            }
            return wishlist.stream()
                    .map(b -> String.format("ID: %s | Название: %s | Цена: %s BYN", b.getId(), b.getTitle(), b.getCost()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Не удалось получить список желаемого.";
        }
    }

    @Tool("Добавить книгу в вишлист пользователя по ID книги (UUID). Используй после рекомендации, если пользователь сказал 'добавь в вишлист', 'хочу её', 'сохрани' и т.п.")
    public String addToWishlist(@P("UUID книги, которую добавить в вишлист") String bookId) {
        log.info("AI tool [addToWishlist] bookId={}", bookId);
        try {
            String username = currentUsername();
            userService.addBookToWishlist(username, UUID.fromString(bookId));
            profileCache.invalidate(username);
            return "Книга успешно добавлена в вишлист.";
        } catch (IllegalArgumentException e) {
            return "Некорректный ID книги.";
        } catch (Exception e) {
            return "Не удалось добавить в вишлист: " + e.getMessage();
        }
    }

    @Tool("Удалить книгу из вишлиста по ID книги (UUID). Используй когда пользователь сказал 'убери из вишлиста', 'удали' и т.п.")
    public String removeFromWishlist(@P("UUID книги для удаления из вишлиста") String bookId) {
        log.info("AI tool [removeFromWishlist] bookId={}", bookId);
        try {
            String username = currentUsername();
            userService.removeBookFromWishlist(username, UUID.fromString(bookId));
            profileCache.invalidate(username);
            return "Книга удалена из вишлиста.";
        } catch (IllegalArgumentException e) {
            return "Некорректный ID книги.";
        } catch (Exception e) {
            return "Не удалось удалить из вишлиста: " + e.getMessage();
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
