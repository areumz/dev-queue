package com.areumz.devqueue.repository.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MemoryEmailTokenRepositoryTest {

    private final MemoryEmailTokenRepository repository = new MemoryEmailTokenRepository();
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);

    private EmailToken token(Long userId, EmailTokenType type, String hash, LocalDateTime createdAt) {
        return new EmailToken(userId, type, hash, createdAt.plusHours(1), createdAt);
    }

    @Test
    void saveAssignsAnIdAndTheTokenIsFoundByHashAndType() {
        EmailToken saved = repository.save(token(1L, EmailTokenType.VERIFY_EMAIL, "h1", now));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByTokenHashAndType("h1", EmailTokenType.VERIFY_EMAIL)).contains(saved);
        assertThat(repository.findByTokenHashAndType("h1", EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void findOpenSkipsUsedTokens() {
        EmailToken open = repository.save(token(1L, EmailTokenType.VERIFY_EMAIL, "h1", now));
        EmailToken used = repository.save(token(1L, EmailTokenType.RESET_PASSWORD, "h2", now));
        used.markUsed(now);
        repository.save(token(2L, EmailTokenType.VERIFY_EMAIL, "h3", now));

        assertThat(repository.findOpenByUserId(1L)).containsExactly(open);
    }

    @Test
    void countCreatedSinceCountsOnlyTheMatchingUserTypeAndTime() {
        repository.save(token(1L, EmailTokenType.VERIFY_EMAIL, "h1", now));
        repository.save(token(1L, EmailTokenType.VERIFY_EMAIL, "h2", now.minusHours(2)));
        repository.save(token(1L, EmailTokenType.RESET_PASSWORD, "h3", now));
        repository.save(token(2L, EmailTokenType.VERIFY_EMAIL, "h4", now));

        assertThat(repository.countCreatedSince(1L, EmailTokenType.VERIFY_EMAIL, now.minusHours(1))).isEqualTo(1);
    }
}
