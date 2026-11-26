package com.areumz.devqueue.repository;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailTokenRepository {
    EmailToken save(EmailToken token);

    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);

    /** 아직 안 쓴 토큰(만료 여부는 상관없다). */
    List<EmailToken> findOpenByUserId(Long userId);

    /** since 이후에 만들어진 토큰 수(썼든 안 썼든 센다). */
    long countCreatedSince(Long userId, EmailTokenType type, LocalDateTime since);
}
