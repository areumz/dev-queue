package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class SignupEmailTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserService userService;

    private ResultActions signup(String username, String password, String email) throws Exception {
        return mockMvc.perform(post("/signup").with(csrf())
                .param("username", username).param("password", password)
                .param("nickname", "닉").param("role", "BACKEND").param("email", email));
    }

    @Test
    void emailIsTrimmedAndLowercasedOnSave() throws Exception {
        signup("carol", "password123", " Carol@Example.COM ").andExpect(redirectedUrl("/login"));
        assertThat(userRepository.findByUsername("carol").orElseThrow().getEmail()).isEqualTo("carol@example.com");
    }

    @Test
    void blankEmailIsSavedAsNull() throws Exception {
        signup("carol", "password123", "   ").andExpect(redirectedUrl("/login"));
        assertThat(userRepository.findByUsername("carol").orElseThrow().getEmail()).isNull();
    }

    @Test
    void twoUsersWithoutEmailAreNotDuplicates() throws Exception {
        signup("carol", "password123", "").andExpect(redirectedUrl("/login"));
        signup("dave", "password123", "").andExpect(redirectedUrl("/login"));
        assertThat(userRepository.findByUsername("dave")).isPresent();
    }

    @Test
    void sameEmailCanBeSavedByManyAccountsUntilItIsVerified() throws Exception {
        userService.signup("alice", "password123", "앨리스", Role.BACKEND, null, "a@x.com");

        signup("bob", "password123", " A@X.com ").andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findByUsername("bob").orElseThrow().getEmail()).isEqualTo("a@x.com");
    }

    @Test
    void invalidEmailFormatShowsMessage() throws Exception {
        signup("carol", "password123", "not-an-email")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이메일 형식이 올바르지 않습니다.")));
        assertThat(userRepository.findByUsername("carol")).isEmpty();
    }

    @Test
    void tooLongEmailIsRejected() throws Exception {
        signup("carol", "password123", "a".repeat(250) + "@x.com")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이메일은 255자 이하로 입력해 주세요.")));
        assertThat(userRepository.findByUsername("carol")).isEmpty();
    }

    @Test
    void sevenCharPasswordIsRejected() throws Exception {
        signup("carol", "1234567", "")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("8자 이상")));
        assertThat(userRepository.findByUsername("carol")).isEmpty();
    }

    @Test
    void eightCharPasswordIsAccepted() throws Exception {
        signup("carol", "12345678", "").andExpect(redirectedUrl("/login"));
    }

    @Test
    void duplicateUsernameStillGoesToUsernameField() throws Exception {
        userService.signup("alice", "password123", "앨리스", Role.BACKEND, null, null);
        signup("alice", "password123", "")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 존재하는 아이디입니다.")));
    }
}
