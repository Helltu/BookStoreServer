package com.bsuir.book_store.users.application;

import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.api.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final long CODE_TTL_SECONDS = 60;

    private final JavaMailSender mailSender;

    private record PendingRegistration(RegisterRequest request, String code, Instant expiresAt) {}

    private final ConcurrentHashMap<String, PendingRegistration> pending = new ConcurrentHashMap<>();

    public void initiate(RegisterRequest request) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        pending.put(request.getEmail(), new PendingRegistration(request, code, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        sendCode(request.getEmail(), code);
    }

    public void resend(String email) {
        PendingRegistration entry = pending.get(email);
        if (entry == null) {
            throw new DomainException("Сначала выполните регистрацию.");
        }
        if (Instant.now().isBefore(entry.expiresAt())) {
            throw new DomainException("Код ещё действителен. Подождите истечения таймера.");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        pending.put(email, new PendingRegistration(entry.request(), code, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        sendCode(email, code);
    }

    public long getSecondsRemaining(String email) {
        PendingRegistration entry = pending.get(email);
        if (entry == null) {
            return 0;
        }
        long remaining = Instant.now().until(entry.expiresAt(), java.time.temporal.ChronoUnit.SECONDS);
        return Math.max(remaining, 0);
    }

    public RegisterRequest verify(String email, String code) {
        PendingRegistration entry = pending.get(email);
        if (entry == null) {
            throw new DomainException("Код не найден. Повторите регистрацию.");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            pending.remove(email);
            throw new DomainException("Код истёк. Повторите регистрацию.");
        }
        if (!entry.code().equals(code)) {
            throw new DomainException("Неверный код подтверждения.");
        }
        pending.remove(email);
        return entry.request();
    }

    @Async
    protected void sendCode(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Код подтверждения регистрации");
            message.setText("Ваш код подтверждения: " + code + "\n\nКод действителен 1 минуту.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
        }
    }
}
