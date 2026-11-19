package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleListQueryCountTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    EntityManager em;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    @Test
    void articleListWithPopupUsesFewQueries() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        User bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
        for (int i = 0; i < 10; i++) {
            saveArticleWithWords(alice, i);
        }
        saveArticleWithWords(bob, 99);
        em.flush();
        em.clear();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        mockMvc.perform(get("/articles").with(TestAuth.loginAs(alice)))
                .andExpect(model().attributeExists("popupWords"));
        em.flush();

        long queries = stats.getPrepareStatementCount();
        assertThat(queries).isLessThanOrEqualTo(5);
    }

    private void saveArticleWithWords(User user, int n) {
        Article article = new Article("글" + n, "https://example.com/" + n, Category.AI, "");
        article.setUserId(user.getId());
        articleRepository.save(article);
        for (int j = 0; j < 3; j++) {
            vocabularyRepository.save(new Vocabulary("w" + n + "_" + j, "뜻", article.getId()));
        }
    }
}
