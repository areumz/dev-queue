package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaEmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);

    List<EmailToken> findByUserIdAndUsedAtIsNull(Long userId);

    long countByUserIdAndTypeAndCreatedAtAfter(Long userId, EmailTokenType type, LocalDateTime since);

    void deleteByUserId(Long userId);
}
