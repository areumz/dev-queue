package com.areumz.devqueue.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizeTest {
    @Test
    void nullBlankAndSpacesBecomeNull() {
        assertThat(UserService.normalizeEmail(null)).isNull();
        assertThat(UserService.normalizeEmail("")).isNull();
        assertThat(UserService.normalizeEmail("   ")).isNull();
    }

    @Test
    void trimsAndLowercases() {
        assertThat(UserService.normalizeEmail("  A@X.Com ")).isEqualTo("a@x.com");
    }
}
