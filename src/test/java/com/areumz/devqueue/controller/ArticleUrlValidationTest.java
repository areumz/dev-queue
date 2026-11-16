package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.service.ArticleService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleUrlValidationTest {

    static final String MESSAGE = "http 또는 https 주소만 저장할 수 있어요";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    ArticleService articleService;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "JavaScript:alert(1)", " javascript:alert(1)",
            "https://", "http:foo", "ftp://example.com", "", "   "})
    void createRejectsNonHttpUrl(String url) throws Exception {
        mockMvc.perform(post("/articles/new").with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "내 제목").param("url", url)
                        .param("category", "AI").param("memo", "내 메모"))
                .andExpect(status().isOk())
                .andExpect(view().name("createArticleForm"))
                .andExpect(content().string(containsString(MESSAGE)))
                .andExpect(content().string(containsString("내 제목")))
                .andExpect(content().string(containsString("내 메모")));
        assertThat(articleService.findMyArticles(alice.getId(), null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "HTTP://EXAMPLE.COM/a"})
    void createAcceptsHttpUrl(String url) throws Exception {
        mockMvc.perform(post("/articles/new").with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "t").param("url", url)
                        .param("category", "AI").param("memo", "m"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/articles"));
        assertThat(articleService.findMyArticles(alice.getId(), null)).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com ", " https://example.com"})
    void createStripsSurroundingWhitespace(String url) throws Exception {
        mockMvc.perform(post("/articles/new").with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "t").param("url", url)
                        .param("category", "AI").param("memo", "m"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/articles"));
        assertThat(articleService.findMyArticles(alice.getId(), null))
                .extracting(Article::getUrl).containsExactly("https://example.com");
    }

    @Test
    void editStripsSurroundingWhitespace() throws Exception {
        Article article = save(alice);
        mockMvc.perform(post("/articles/" + article.getId() + "/edit")
                        .with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "t").param("url", " https://example.org ").param("category", "AI"))
                .andExpect(status().is3xxRedirection());
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getUrl())
                .isEqualTo("https://example.org");
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "JavaScript:alert(1)", " javascript:alert(1)",
            "https://", "http:foo", "ftp://example.com", "", "   "})
    void editRejectsNonHttpUrl(String url) throws Exception {
        Article article = save(alice);
        mockMvc.perform(post("/articles/" + article.getId() + "/edit")
                        .with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "새 제목").param("url", url).param("category", "ETC"))
                .andExpect(status().isOk())
                .andExpect(view().name("editArticleForm"))
                .andExpect(content().string(containsString(MESSAGE)))
                .andExpect(content().string(containsString("새 제목")))
                .andExpect(content().string(containsString("/articles/" + article.getId() + "/edit")));
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getUrl())
                .isEqualTo("https://example.com");
    }

    @Test
    void editOfOthersArticleIsStill404WithBadUrl() throws Exception {
        Article article = save(alice);
        mockMvc.perform(post("/articles/" + article.getId() + "/edit")
                        .with(TestAuth.loginAs(bob)).with(csrf())
                        .param("title", "t").param("url", "javascript:alert(1)").param("category", "AI"))
                .andExpect(status().isNotFound());
    }

    private Article save(User owner) {
        Article article = new Article("원래 제목", "https://example.com", Category.AI, "m");
        article.setUserId(owner.getId());
        return articleRepository.save(article);
    }
}
