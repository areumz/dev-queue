package com.areumz.devqueue.repository;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailTokenRepository {
    EmailToken save(EmailToken token);

    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);

    /** 미사용 토큰 (만료 여부는 상관x). */
    List<EmailToken> findOpenByUserId(Long userId);

    /** since 이후에 만들어진 토큰 수(썼든 안 썼든 카운트). */
    long countCreatedSince(Long userId, EmailTokenType type, LocalDateTime since);

    /** 탈퇴할 때 그 사용자의 토큰을 모두 삭제. */
    void deleteByUserId(Long userId);
}
