package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.repository.EmailTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("jpa")
public class JpaEmailTokenRepository implements EmailTokenRepository {
    private final SpringDataJpaEmailTokenRepository springDataJpaEmailTokenRepository;

    public JpaEmailTokenRepository(SpringDataJpaEmailTokenRepository springDataJpaEmailTokenRepository) {
        this.springDataJpaEmailTokenRepository = springDataJpaEmailTokenRepository;
    }

    @Override
    public EmailToken save(EmailToken token) {
        return springDataJpaEmailTokenRepository.save(token);
    }

    @Override
    public Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type) {
        return springDataJpaEmailTokenRepository.findByTokenHashAndType(tokenHash, type);
    }

    @Override
    public List<EmailToken> findOpenByUserId(Long userId) {
        return springDataJpaEmailTokenRepository.findByUserIdAndUsedAtIsNull(userId);
    }

    @Override
    public long countCreatedSince(Long userId, EmailTokenType type, LocalDateTime since) {
        return springDataJpaEmailTokenRepository.countByUserIdAndTypeAndCreatedAtAfter(userId, type, since);
    }
}
