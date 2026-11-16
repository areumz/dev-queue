package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class LoginFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userService.signup("alice", "password123", "앨리스", Role.BACKEND, null);
    }

    @Test
    void loginSucceedsAndRotatesSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();
        MvcResult result = mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "alice").param("password", "password123"))
                .andExpect(redirectedUrl("/articles"))
                .andReturn();
        assertThat(result.getRequest().getSession().getId()).isNotEqualTo(before);
    }

    @Test
    void sessionHoldsLoginUserWithoutPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "password123"))
                .andReturn();
        HttpSession session = result.getRequest().getSession();
        assertThat(session.getAttribute("loginUser")).isNull();
        SecurityContext ctx = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        LoginUser principal = (LoginUser) ctx.getAuthentication().getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void wrongPasswordRedirectsToLoginError() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginErrorPageShowsMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        mockMvc.perform(post("/articles/new").with(TestAuth.loginAs(alice))
                        .param("title", "t").param("url", "https://example.com")
                        .param("category", "AI").param("memo", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutRedirectsToLogin() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        mockMvc.perform(post("/logout").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void errorPageIsOpenToAnonymous() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());
    }
}
