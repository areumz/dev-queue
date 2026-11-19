package com.areumz.devqueue.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ErrorPageTest {

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
    void unknownArticleShowsNotFoundPage() throws Exception {
        mockMvc.perform(get("/articles/999999").with(TestAuth.loginAs(alice)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("찾을 수 없는 페이지예요")));
    }

    @Test
    void missingParameterShowsBadRequestPage() throws Exception {
        Article article = new Article("t", "https://example.com", Category.AI, null);
        article.setUserId(alice.getId());
        article = articleRepository.save(article);

        mockMvc.perform(post("/articles/" + article.getId() + "/edit").with(TestAuth.loginAs(alice)).with(csrf())
                        .param("title", "t").param("url", "https://example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("요청이 올바르지 않아요")));
    }

    @Test
    void serverErrorPageIsFriendly() throws Exception {
        mockMvc.perform(get("/error").accept(org.springframework.http.MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("잠시 후 다시 시도해 주세요")))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at com."))));
    }
}
