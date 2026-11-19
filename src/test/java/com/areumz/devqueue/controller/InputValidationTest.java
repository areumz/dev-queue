package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class InputValidationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ArticleRepository articleRepository;

    User alice;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
    }

    @Test
    void editWithoutCategoryIsBadRequest() throws Exception {
        Article article = new Article("제목", "https://example.com", Category.AI, "메모");
        article.setUserId(alice.getId());
        article = articleRepository.save(article);

        mockMvc.perform(post("/articles/" + article.getId() + "/edit")
                        .with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "새 제목").param("url", "https://example.com"))
                .andExpect(status().isBadRequest());

        assertThat(articleRepository.findById(article.getId()).orElseThrow().getCategory())
                .isEqualTo(Category.AI);
    }

    @Test
    void duplicateUsernameShowsMessage() throws Exception {
        mockMvc.perform(post("/signup").with(csrf())
                        .param("username", "alice").param("password", "pw1234")
                        .param("nickname", "다른앨리스").param("role", "BACKEND"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 존재하는 아이디입니다.")));

        assertThat(userRepository.findAll().stream().filter(u -> "alice".equals(u.getUsername())).count())
                .isEqualTo(1);
    }

    @Test
    void signupValidationErrorKeepsRoleOptions() throws Exception {
        mockMvc.perform(post("/signup").with(csrf())
                        .param("username", "carol").param("password", "")
                        .param("nickname", "캐롤").param("role", "BACKEND"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("백엔드 개발자")));
    }
}
