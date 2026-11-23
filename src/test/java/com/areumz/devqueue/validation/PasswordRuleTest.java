package com.areumz.devqueue.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordRuleTest {
    private final PasswordRuleValidator validator = new PasswordRuleValidator();

    @Test
    void sevenCharsRejected() {
        assertThat(validator.isValid("1234567", null)).isFalse();
    }

    @Test
    void eightCharsAccepted() {
        assertThat(validator.isValid("12345678", null)).isTrue();
    }

    @Test
    void seventyTwoBytesAccepted() {
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
    }

    @Test
    void seventyThreeBytesRejected() {
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
    }

    @Test
    void koreanTwentyFourCharsAccepted() {
        assertThat(validator.isValid("가".repeat(24), null)).isTrue();
    }

    @Test
    void koreanTwentyFiveCharsRejected() {
        assertThat(validator.isValid("가".repeat(25), null)).isFalse();
    }

    @Test
    void blankAndNullAreLeftToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
