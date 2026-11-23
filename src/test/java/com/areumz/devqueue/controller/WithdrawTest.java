package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class WithdrawTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    User alice;
    User bob;
    Article aliceArticle1;
    Article aliceArticle2;
    Article bobArticle;

    @BeforeEach
    void setUp() {
        alice = userService.signup("alice", "password123", "앨리스", Role.BACKEND, null, null);
        bob = userService.signup("bob", "password123", "밥", Role.BACKEND, null, null);
        aliceArticle1 = saveArticle(alice, "a1");
        aliceArticle2 = saveArticle(alice, "a2");
        bobArticle = saveArticle(bob, "b1");
        vocabularyRepository.save(new Vocabulary("w1", "뜻", aliceArticle1.getId()));
        vocabularyRepository.save(new Vocabulary("w2", "뜻", aliceArticle1.getId()));
        vocabularyRepository.save(new Vocabulary("w3", "뜻", aliceArticle2.getId()));
        vocabularyRepository.save(new Vocabulary("bw", "뜻", bobArticle.getId()));
    }

    private Article saveArticle(User owner, String title) {
        Article article = new Article(title, "https://example.com/" + title, Category.AI, "");
        article.setUserId(owner.getId());
        return articleRepository.save(article);
    }

    private ResultActions withdraw(User who, String password) throws Exception {
        return mockMvc.perform(post("/mypage/withdraw").with(TestAuth.loginAs(who)).with(csrf())
                .param("password", password));
    }

    @Test
    void withdrawDeletesOnlyMyWordsArticlesAndAccount() throws Exception {
        withdraw(alice, "password123").andExpect(redirectedUrl("/login?withdrawn"));

        assertThat(userRepository.findByUsername("alice")).isEmpty();
        assertThat(articleRepository.findByUserId(alice.getId())).isEmpty();
        assertThat(vocabularyRepository.findByArticleId(aliceArticle1.getId())).isEmpty();
        assertThat(vocabularyRepository.findByArticleId(aliceArticle2.getId())).isEmpty();

        assertThat(userRepository.findByUsername("bob")).isPresent();
        assertThat(articleRepository.findByUserId(bob.getId())).hasSize(1);
        assertThat(vocabularyRepository.findByArticleId(bobArticle.getId())).hasSize(1);
    }

    @Test
    void wrongPasswordDeletesNothing() throws Exception {
        withdraw(alice, "wrong-password")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("비밀번호가 올바르지 않습니다.")));

        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(articleRepository.findByUserId(alice.getId())).hasSize(2);
        assertThat(vocabularyRepository.findByArticleId(aliceArticle1.getId())).hasSize(2);
        assertThat(userRepository.findByUsername("bob")).isPresent();
        assertThat(articleRepository.findByUserId(bob.getId())).hasSize(1);
    }

    @Test
    void cannotLoginAfterWithdrawal() throws Exception {
        withdraw(alice, "password123");
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "password123"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void sessionIsEndedAfterWithdrawal() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice").param("password", "password123"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(post("/mypage/withdraw").session(session).with(csrf())
                        .param("password", "password123"))
                .andExpect(redirectedUrl("/login?withdrawn"));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void loginPageShowsWithdrawnMessage() throws Exception {
        mockMvc.perform(get("/login").param("withdrawn", ""))
                .andExpect(content().string(containsString("탈퇴가 완료되었어요.")));
    }

    @Test
    void postWithoutCsrfIsForbiddenAndDeletesNothing() throws Exception {
        mockMvc.perform(post("/mypage/withdraw").with(TestAuth.loginAs(alice))
                        .param("password", "password123"))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByUsername("alice")).isPresent();
    }

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/mypage/withdraw")).andExpect(redirectedUrl("/login"));
    }
}
