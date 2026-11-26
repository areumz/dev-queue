package com.areumz.devqueue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

/** 1회용 토큰 한 건. 원본은 저장하지 않고 해시만 저장한다. userId는 FK 없는 Long(프로젝트 기존 방식). */
@Entity
public class EmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailTokenType type;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected EmailToken() {
    }

    public EmailToken(Long userId, EmailTokenType type, String tokenHash,
                      LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.userId = userId;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public EmailTokenType getType() {
        return type;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 미사용 & 만료 전 */
    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    /** 미사용 (만료 여부는 상관x). */
    public boolean isOpen() {
        return usedAt == null;
    }

    public void markUsed(LocalDateTime now) {
        if (usedAt == null) {
            usedAt = now;
        }
    }
}
