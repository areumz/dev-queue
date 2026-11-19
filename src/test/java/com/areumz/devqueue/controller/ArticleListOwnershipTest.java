package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleListOwnershipTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    @Test
    void listAndPopupContainOnlyCallersData() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        User bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
        for (int i = 0; i < 10; i++) {
            saveArticleWithWords(alice, Category.AI, i);
        }
        for (int i = 0; i < 5; i++) {
            saveArticleWithWords(bob, Category.AI, 90 + i);
        }

        Map<String, Object> model = mockMvc.perform(get("/articles").with(TestAuth.loginAs(alice)))
                .andReturn().getModelAndView().getModel();

        List<Article> articles = articlesOf(model);
        assertThat(articles).hasSize(10);
        assertThat(articles).allMatch(a -> a.getUserId().equals(alice.getId()));
    }

    @Test
    void popupContainsOnlyCallersWords() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        User bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
        Article aliceArticle = new Article("글a", "https://example.com/a", Category.AI, "");
        aliceArticle.setUserId(alice.getId());
        articleRepository.save(aliceArticle);
        vocabularyRepository.save(new Vocabulary("a0", "뜻", aliceArticle.getId()));
        vocabularyRepository.save(new Vocabulary("a1", "뜻", aliceArticle.getId()));
        for (int i = 0; i < 4; i++) {
            saveArticleWithWords(bob, Category.AI, 90 + i);
        }

        Map<String, Object> model = mockMvc.perform(get("/articles").with(TestAuth.loginAs(alice)))
                .andReturn().getModelAndView().getModel();

        assertThat(popupOf(model)).extracting(Vocabulary::getWord).containsExactlyInAnyOrder("a0", "a1");
    }

    @Test
    void categoryFilterReturnsOnlyCallersArticlesInThatCategory() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        User bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
        saveArticleWithWords(bob, Category.AI, 1);
        Article aliceAi = saveArticleWithWords(alice, Category.AI, 2);
        saveArticleWithWords(alice, Category.JOB, 3);

        Map<String, Object> model = mockMvc
                .perform(get("/articles").param("category", "AI").with(TestAuth.loginAs(alice)))
                .andReturn().getModelAndView().getModel();

        assertThat(articlesOf(model)).extracting(Article::getId).containsExactly(aliceAi.getId());
    }

    @SuppressWarnings("unchecked")
    private List<Article> articlesOf(Map<String, Object> model) {
        return (List<Article>) model.get("articles");
    }

    @SuppressWarnings("unchecked")
    private List<Vocabulary> popupOf(Map<String, Object> model) {
        return (List<Vocabulary>) model.get("popupWords");
    }

    private Article saveArticleWithWords(User user, Category category, int n) {
        Article article = new Article("글" + n, "https://example.com/" + n, category, "");
        article.setUserId(user.getId());
        articleRepository.save(article);
        for (int j = 0; j < 3; j++) {
            vocabularyRepository.save(new Vocabulary("w" + n + "_" + j, "뜻", article.getId()));
        }
        return article;
    }
}
