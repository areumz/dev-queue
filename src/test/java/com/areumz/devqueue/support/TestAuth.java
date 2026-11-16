package com.areumz.devqueue.support;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.domain.User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Mimics a logged-in user the Spring Security way. */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor loginAs(User user) {
        return SecurityMockMvcRequestPostProcessors.user(LoginUser.from(user));
    }
}
