package com.areumz.devqueue.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Temporary: proves a failing test blocks the merge. Reverted right after the check. */
class DeliberateFailureTest {

    @Test
    void failsOnPurpose() {
        assertThat(1).isEqualTo(2);
    }
}
