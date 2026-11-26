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
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MyPageTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null, "alice@x.com"));
        bob = userRepository.save(new User("bob", "x", "밥", Role.FRONTEND, null, "bob@x.com"));
    }

    private ResultActions update(User who, String nickname, String role, String email) throws Exception {
        return mockMvc.perform(post("/mypage").with(TestAuth.loginAs(who)).with(csrf())
                .param("nickname", nickname).param("role", role).param("email", email));
    }

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/mypage")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void formShowsCurrentValues() throws Exception {
        mockMvc.perform(get("/mypage").with(TestAuth.loginAs(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"alice\"")))
                .andExpect(content().string(containsString("앨리스")))
                .andExpect(content().string(containsString("alice@x.com")));
    }

    @Test
    void updateChangesNicknameRoleAndNormalizedEmail() throws Exception {
        update(alice, "새닉", "FRONTEND", " New@X.com ").andExpect(redirectedUrl("/mypage"));
        em.flush();
        em.clear();
        User saved = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(saved.getNickname()).isEqualTo("새닉");
        assertThat(saved.getRole()).isEqualTo(Role.FRONTEND);
        assertThat(saved.getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void blankEmailClearsIt() throws Exception {
        update(alice, "앨리스", "BACKEND", "").andExpect(redirectedUrl("/mypage"));
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getEmail()).isNull();
    }

    @Test
    void anotherAccountsUnverifiedEmailCanBeSaved() throws Exception {
        update(alice, "새닉", "BACKEND", "BOB@x.com").andExpect(redirectedUrl("/mypage"));
        em.flush();
        em.clear();
        User saved = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(saved.getNickname()).isEqualTo("새닉");
        assertThat(saved.getEmail()).isEqualTo("bob@x.com");
        assertThat(saved.isEmailVerified()).isFalse();
    }

    @Test
    void keepingOwnEmailIsAllowed() throws Exception {
        update(alice, "새닉", "BACKEND", "Alice@x.com").andExpect(redirectedUrl("/mypage"));
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getNickname()).isEqualTo("새닉");
    }

    @Test
    void blankNicknameShowsMessage() throws Exception {
        update(alice, "  ", "BACKEND", "alice@x.com")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("닉네임을 입력해주세요.")));
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getNickname()).isEqualTo("앨리스");
    }

    @Test
    void invalidEmailFormatShowsMessage() throws Exception {
        update(alice, "앨리스", "BACKEND", "not-an-email")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이메일 형식이 올바르지 않습니다.")));
    }

    @Test
    void roleDetailIsKeptOnlyForEtc() throws Exception {
        alice.updateProfile("앨리스", Role.ETC, "X", "alice@x.com");
        mockMvc.perform(post("/mypage").with(TestAuth.loginAs(alice)).with(csrf())
                .param("nickname", "앨리스").param("role", "ETC").param("roleDetail", "Y")
                .param("email", "alice@x.com")).andExpect(redirectedUrl("/mypage"));
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getRoleDetail()).isEqualTo("Y");

        mockMvc.perform(post("/mypage").with(TestAuth.loginAs(alice)).with(csrf())
                .param("nickname", "앨리스").param("role", "BACKEND").param("roleDetail", "X")
                .param("email", "alice@x.com")).andExpect(redirectedUrl("/mypage"));
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getRoleDetail()).isNull();
    }

    @Test
    void tooLongEmailIsRejected() throws Exception {
        update(alice, "새닉", "BACKEND", "a".repeat(250) + "@x.com")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이메일은 255자 이하로 입력해 주세요.")));
        em.flush();
        em.clear();
        User saved = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(saved.getNickname()).isEqualTo("앨리스");
        assertThat(saved.getEmail()).isEqualTo("alice@x.com");
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/mypage").with(TestAuth.loginAs(alice))
                        .param("nickname", "새닉").param("role", "BACKEND"))
                .andExpect(status().isForbidden());
    }

    @Test
    void articleListOfDeletedUserGetsNotFound() throws Exception {
        User ghost = new User("ghost", "x", "유령", Role.BACKEND, null);
        ghost.setId(999_999L);
        mockMvc.perform(get("/articles").with(TestAuth.loginAs(ghost)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sessionOfDeletedUserGetsNotFound() throws Exception {
        User ghost = new User("ghost", "x", "유령", Role.BACKEND, null);
        ghost.setId(999_999L);
        mockMvc.perform(get("/mypage").with(TestAuth.loginAs(ghost)))
                .andExpect(status().isNotFound());
    }
}
