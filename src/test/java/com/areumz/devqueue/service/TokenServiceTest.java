package com.areumz.devqueue.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.repository.EmailTokenRepository;
import com.areumz.devqueue.repository.jpa.SpringDataJpaEmailTokenRepository;
import com.areumz.devqueue.support.IntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class TokenServiceTest {

    private static final Long USER = 1L;
    private static final Long OTHER_USER = 2L;

    @Autowired
    TokenService tokenService;

    @Autowired
    EmailTokenRepository tokenRepository;

    @Autowired
    SpringDataJpaEmailTokenRepository rows;

    @Autowired
    Clock clock;

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Test
    void issueStoresOnlyTheHash() {
        String raw = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();

        List<EmailToken> all = rows.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTokenHash())
                .isEqualTo(TokenService.hash(raw))
                .isNotEqualTo(raw)
                .hasSize(64);
    }

    @Test
    void everyIssuedTokenIsDifferent() {
        String first = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String second = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void validTokenIsFound() {
        String raw = tokenService.issue(USER, EmailTokenType.RESET_PASSWORD).orElseThrow();
        assertThat(tokenService.findValid(raw, EmailTokenType.RESET_PASSWORD)).isPresent();
    }

    @Test
    void unknownBlankAndNullTokensAreNotFound() {
        assertThat(tokenService.findValid("nope", EmailTokenType.RESET_PASSWORD)).isEmpty();
        assertThat(tokenService.findValid("", EmailTokenType.RESET_PASSWORD)).isEmpty();
        assertThat(tokenService.findValid("   ", EmailTokenType.RESET_PASSWORD)).isEmpty();
        assertThat(tokenService.findValid(null, EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void tokenOfAnotherTypeIsNotFound() {
        String raw = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        assertThat(tokenService.findValid(raw, EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void expiredTokenIsNotFound() {
        tokenRepository.save(new EmailToken(USER, EmailTokenType.RESET_PASSWORD,
                TokenService.hash("expired-raw"), now().minusMinutes(1), now().minusMinutes(31)));
        assertThat(tokenService.findValid("expired-raw", EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void usedTokenIsNotFound() {
        String raw = tokenService.issue(USER, EmailTokenType.RESET_PASSWORD).orElseThrow();
        tokenService.consume(tokenService.findValid(raw, EmailTokenType.RESET_PASSWORD).orElseThrow());
        assertThat(tokenService.findValid(raw, EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void newIssueInvalidatesThePreviousOneOfTheSameUserAndType() {
        String first = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String second = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        assertThat(tokenService.findValid(first, EmailTokenType.VERIFY_EMAIL)).isEmpty();
        assertThat(tokenService.findValid(second, EmailTokenType.VERIFY_EMAIL)).isPresent();
    }

    @Test
    void newIssueKeepsOtherTypesAndOtherUsers() {
        String verify = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String otherUser = tokenService.issue(OTHER_USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String reset = tokenService.issue(USER, EmailTokenType.RESET_PASSWORD).orElseThrow();
        tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();   // verify를 한 번 더 발급

        assertThat(tokenService.findValid(otherUser, EmailTokenType.VERIFY_EMAIL)).isPresent();
        assertThat(tokenService.findValid(verify, EmailTokenType.VERIFY_EMAIL)).isEmpty();   // 같은 사용자·종류만 무효
        assertThat(tokenService.findValid(reset, EmailTokenType.RESET_PASSWORD)).isPresent();
    }

    @Test
    void fourthRequestInAnHourIsBlockedAndKeepsTheLastTokenValid() {
        tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String third = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();

        assertThat(tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL)).isEmpty();
        // 제한에 걸린 요청이 사용자의 유효한 링크를 죽이면 안 된다
        assertThat(tokenService.findValid(third, EmailTokenType.VERIFY_EMAIL)).isPresent();
    }

    @Test
    void limitIsPerUserAndPerType() {
        for (int i = 0; i < 3; i++) {
            tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        }
        assertThat(tokenService.issue(USER, EmailTokenType.RESET_PASSWORD)).isPresent();
        assertThat(tokenService.issue(OTHER_USER, EmailTokenType.VERIFY_EMAIL)).isPresent();
    }

    @Test
    void requestsOlderThanOneHourAreNotCounted() {
        for (int i = 0; i < 3; i++) {
            EmailToken old = new EmailToken(USER, EmailTokenType.VERIFY_EMAIL,
                    TokenService.hash("old" + i), now().minusHours(1), now().minusHours(2));
            old.markUsed(now().minusHours(2));
            tokenRepository.save(old);
        }
        assertThat(tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL)).isPresent();
    }

    @Test
    void invalidateAllOpenKillsEveryTypeOfThatUserOnly() {
        String verify = tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String reset = tokenService.issue(USER, EmailTokenType.RESET_PASSWORD).orElseThrow();
        String other = tokenService.issue(OTHER_USER, EmailTokenType.RESET_PASSWORD).orElseThrow();

        tokenService.invalidateAllOpen(USER);

        assertThat(tokenService.findValid(verify, EmailTokenType.VERIFY_EMAIL)).isEmpty();
        assertThat(tokenService.findValid(reset, EmailTokenType.RESET_PASSWORD)).isEmpty();
        assertThat(tokenService.findValid(other, EmailTokenType.RESET_PASSWORD)).isPresent();
    }

    @Test
    void lifetimesAreADayForVerifyAndHalfAnHourForReset() {
        tokenService.issue(USER, EmailTokenType.VERIFY_EMAIL).orElseThrow();
        tokenService.issue(USER, EmailTokenType.RESET_PASSWORD).orElseThrow();

        for (EmailToken token : rows.findAll()) {
            Duration lifetime = Duration.between(token.getCreatedAt(), token.getExpiresAt());
            if (token.getType() == EmailTokenType.VERIFY_EMAIL) {
                assertThat(lifetime).isEqualTo(Duration.ofHours(24));
            } else {
                assertThat(lifetime).isEqualTo(Duration.ofMinutes(30));
            }
        }
    }
}
