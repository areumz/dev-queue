package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.repository.EmailTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증, 비밀번호 재설정이 같이 쓰는 1회용 토큰. 원본은 메일 링크에만 있고 DB에는 해시만 저장. */
@Service
public class TokenService {

    static final int MAX_PER_WINDOW = 3;
    static final Duration WINDOW = Duration.ofHours(1);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailTokenRepository tokenRepository;
    private final Clock clock;

    public TokenService(EmailTokenRepository tokenRepository, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }

    /** 토큰 원본의 SHA-256 해시(hex 64자). 같은 입력은 항상 같은 값이라 조회에 사용. */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없다", e);
        }
    }

    /**
     * 새 토큰 생성 후 원본 return. 같은 사용자·같은 종류가 1시간에 3번을 넘으면 빈 값.
     * 제한 확인이 먼저: 제한에 걸린 요청이 사용자의 유효한 링크를 무효로 만들면 안 됨.
     */
    @Transactional
    public Optional<String> issue(Long userId, EmailTokenType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (tokenRepository.countCreatedSince(userId, type, now.minus(WINDOW)) >= MAX_PER_WINDOW) {
            return Optional.empty();
        }
        invalidateOpen(userId, type);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new EmailToken(userId, type, hash(rawToken), now.plus(type.getTtl()), now));
        return Optional.of(rawToken);
    }

    /** 있고, 종류가 맞고, 안 썼고, 만료 전인 토큰. 아니면 이유와 상관없이 빈 값. */
    public Optional<EmailToken> findValid(String rawToken, EmailTokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return tokenRepository.findByTokenHashAndType(hash(rawToken), type)
                .filter(token -> token.isUsable(now));
    }

    @Transactional
    public void consume(EmailToken token) {
        token.markUsed(LocalDateTime.now(clock));
        tokenRepository.save(token);
    }

    /** 그 사용자의 안 쓴 토큰 중 해당 종류만 무효로 만듦. */
    @Transactional
    public void invalidateOpen(Long userId, EmailTokenType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (EmailToken token : tokenRepository.findOpenByUserId(userId)) {
            if (token.getType() == type) {
                token.markUsed(now);
                tokenRepository.save(token);
            }
        }
    }

    /** 그 사용자의 안 쓴 토큰을 종류와 상관없이 모두 무효로 만듦. */
    @Transactional
    public void invalidateAllOpen(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (EmailToken token : tokenRepository.findOpenByUserId(userId)) {
            token.markUsed(now);
            tokenRepository.save(token);
        }
    }

    /** 탈퇴할 때 그 사용자의 토큰(해시)을 모두 지운다. */
    @Transactional
    public void deleteAllOf(Long userId) {
        tokenRepository.deleteByUserId(userId);
    }
}
