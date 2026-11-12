package com.areumz.devqueue.support;

import com.areumz.devqueue.domain.User;
import org.springframework.mock.web.MockHttpSession;

/**
 * Mimics the current session-based login.
 * Temporary: replace with Spring Security test support in v2-0b.
 */
public final class TestSessions {

    private TestSessions() {
    }

    public static MockHttpSession loginAs(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        return session;
    }
}
