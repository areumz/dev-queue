package com.areumz.devqueue.domain;

import java.time.Duration;

public enum EmailTokenType {
    VERIFY_EMAIL(Duration.ofHours(24)),
    RESET_PASSWORD(Duration.ofMinutes(30));

    private final Duration ttl;

    EmailTokenType(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getTtl() {
        return ttl;
    }
}
