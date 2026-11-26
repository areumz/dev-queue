package com.areumz.devqueue.repository.memory;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.repository.EmailTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class MemoryEmailTokenRepository implements EmailTokenRepository {
    private final ConcurrentHashMap<Long, EmailToken> tokens = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0L);

    @Override
    public EmailToken save(EmailToken token) {
        if (token.getId() == null) {
            token.setId(sequence.incrementAndGet());
        }
        tokens.put(token.getId(), token);
        return token;
    }

    @Override
    public Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type) {
        return tokens.values().stream()
                .filter(token -> token.getTokenHash().equals(tokenHash) && token.getType() == type)
                .findFirst();
    }

    @Override
    public List<EmailToken> findOpenByUserId(Long userId) {
        return tokens.values().stream()
                .filter(token -> token.getUserId().equals(userId) && token.isOpen())
                .toList();
    }

    @Override
    public long countCreatedSince(Long userId, EmailTokenType type, LocalDateTime since) {
        return tokens.values().stream()
                .filter(token -> token.getUserId().equals(userId)
                        && token.getType() == type
                        && token.getCreatedAt().isAfter(since))
                .count();
    }
}
