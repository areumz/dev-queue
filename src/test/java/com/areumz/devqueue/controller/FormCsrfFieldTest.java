package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class FormCsrfFieldTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    User alice;
    Article article;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "hashed", "앨리스", Role.BACKEND, null));
        article = new Article("글", "https://example.com", Category.AI, "메모");
        article.setUserId(alice.getId());
        article = articleRepository.save(article);
        vocabularyRepository.save(new Vocabulary("word", "뜻", article.getId()));
    }

    @Test
    void loginFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/login", false));
    }

    @Test
    void signupFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/signup", false));
    }

    @Test
    void createArticleFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/articles/new", true));
    }

    @Test
    void editArticleFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/articles/" + article.getId() + "/edit", true));
    }

    @Test
    void articleDetailFormsHaveCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/articles/" + article.getId(), true));
    }

    @Test
    void listPageFormsHaveCsrfFields() throws Exception {
        String html = html("/articles", true);
        assertThat(html).contains("/vocabularies/memorize");
        assertEveryFormHasCsrfField(html);
    }

    @Test
    void mypageFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/mypage", true));
    }

    @Test
    void passwordChangeFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/mypage/password", true));
    }

    @Test
    void withdrawFormHasCsrfFields() throws Exception {
        assertEveryFormHasCsrfField(html("/mypage/withdraw", true));
    }

    @Test
    void mypageResendFormHasCsrfField() throws Exception {
        User eve = userRepository.save(new User("eve", "hashed", "이브", Role.BACKEND, null, "eve@x.com"));
        String html = mockMvc.perform(get("/mypage").with(TestAuth.loginAs(eve)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("/mypage/verify-email");
        assertEveryFormHasCsrfField(html);
    }

    private String html(String path, boolean login) throws Exception {
        var request = get(path);
        if (login) {
            request = request.with(TestAuth.loginAs(alice));
        }
        return mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static void assertEveryFormHasCsrfField(String html) {
        String[] forms = html.split("(?i)<form");
        assertThat(forms.length).isGreaterThan(1);
        for (int i = 1; i < forms.length; i++) {
            String body = forms[i].substring(0, forms[i].indexOf("</form>"));
            assertThat(body).contains("name=\"_csrf\"");
        }
    }
}
