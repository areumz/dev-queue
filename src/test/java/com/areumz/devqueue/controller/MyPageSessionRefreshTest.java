package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MyPageSessionRefreshTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    private MockHttpSession loginAsAlice() throws Exception {
        userService.signup("alice", "password123", "앨리스", Role.BACKEND, null, null);
        return (MockHttpSession) mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "password123"))
                .andReturn().getRequest().getSession();
    }

    @Test
    void newNicknameShowsInTheSameSession() throws Exception {
        MockHttpSession session = loginAsAlice();

        mockMvc.perform(post("/mypage").session(session).with(csrf())
                        .param("nickname", "새닉네임").param("role", "BACKEND"))
                .andExpect(redirectedUrl("/mypage"));

        mockMvc.perform(get("/articles").session(session))
                .andExpect(content().string(containsString("새닉네임님")))
                .andExpect(content().string(not(containsString("앨리스님"))));
    }

    @Test
    void refreshedSessionUserHasNoPasswordAndSameIdentity() throws Exception {
        MockHttpSession session = loginAsAlice();
        mockMvc.perform(post("/mypage").session(session).with(csrf())
                .param("nickname", "새닉네임").param("role", "BACKEND"));

        SecurityContext ctx = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        LoginUser principal = (LoginUser) ctx.getAuthentication().getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getNickname()).isEqualTo("새닉네임");
        assertThat(principal.getPassword()).isNull();
    }
}
