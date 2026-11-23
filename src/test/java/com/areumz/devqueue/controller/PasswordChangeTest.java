package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class PasswordChangeTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        userService.signup("alice", "password123", "앨리스", Role.BACKEND, null, null);
    }

    private MockHttpSession login(String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", password))
                .andReturn().getRequest().getSession();
    }

    private ResultActions change(MockHttpSession session, String current, String next) throws Exception {
        return mockMvc.perform(post("/mypage/password").session(session).with(csrf())
                .param("currentPassword", current).param("newPassword", next));
    }

    private String storedHash() {
        return userRepository.findByUsername("alice").orElseThrow().getPassword();
    }

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/mypage/password")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void successLetsNewPasswordLoginAndBlocksOldOne() throws Exception {
        change(login("password123"), "password123", "newpassword456")
                .andExpect(redirectedUrl("/mypage"));
        em.flush();
        em.clear();

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "newpassword456"))
                .andExpect(redirectedUrl("/articles"));
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "password123"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void successRotatesSessionIdOfCurrentBrowser() throws Exception {
        MockHttpSession session = login("password123");
        String before = session.getId();
        change(session, "password123", "newpassword456").andExpect(redirectedUrl("/mypage"));
        assertThat(session.getId()).isNotEqualTo(before);
    }

    @Test
    void wrongCurrentPasswordShowsMessageAndChangesNothing() throws Exception {
        String before = storedHash();
        change(login("password123"), "wrong-password", "newpassword456")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("비밀번호가 올바르지 않습니다.")));
        assertThat(storedHash()).isEqualTo(before);
    }

    @Test
    void tooShortNewPasswordIsRejected() throws Exception {
        String before = storedHash();
        change(login("password123"), "password123", "1234567")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("8자 이상")));
        assertThat(storedHash()).isEqualTo(before);
    }

    @Test
    void tooLongKoreanNewPasswordIsRejected() throws Exception {
        String before = storedHash();
        change(login("password123"), "password123", "가".repeat(25))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("72바이트")));
        assertThat(storedHash()).isEqualTo(before);
    }

    @Test
    void postWithoutCsrfIsForbiddenAndChangesNothing() throws Exception {
        MockHttpSession session = login("password123");
        String before = storedHash();
        mockMvc.perform(post("/mypage/password").session(session)
                        .param("currentPassword", "password123").param("newPassword", "newpassword456"))
                .andExpect(status().isForbidden());
        assertThat(storedHash()).isEqualTo(before);
    }

    @Test
    void blankCurrentPasswordShowsMessage() throws Exception {
        change(login("password123"), "", "newpassword456")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("현재 비밀번호를 입력해주세요.")));
    }
}
