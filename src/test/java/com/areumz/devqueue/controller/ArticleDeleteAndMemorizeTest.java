package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleDeleteAndMemorizeTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void deletingArticleDeletesItsWordsOnly() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        Article a = saveArticle(alice, "A");
        Article b = saveArticle(alice, "B");
        vocabularyRepository.save(new Vocabulary("a1", "뜻", a.getId()));
        vocabularyRepository.save(new Vocabulary("a2", "뜻", a.getId()));
        vocabularyRepository.save(new Vocabulary("b1", "뜻", b.getId()));

        mockMvc.perform(post("/articles/" + a.getId() + "/delete")
                .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();

        assertThat(articleRepository.findById(a.getId())).isEmpty();
        assertThat(articleRepository.findById(b.getId())).isPresent();
        assertThat(vocabularyRepository.findByArticleId(a.getId())).isEmpty();
        assertThat(vocabularyRepository.findByArticleId(b.getId())).hasSize(1);
    }

    @Test
    void nonOwnerDeleteKeepsWords() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        User bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
        Article a = saveArticle(alice, "A");
        vocabularyRepository.save(new Vocabulary("a1", "뜻", a.getId()));

        mockMvc.perform(post("/articles/" + a.getId() + "/delete")
                .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        entityManager.flush();
        entityManager.clear();

        assertThat(articleRepository.findById(a.getId())).isPresent();
        assertThat(vocabularyRepository.findByArticleId(a.getId())).hasSize(1);
    }

    @Test
    void memorizeTwiceStaysMemorized() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        Vocabulary v = vocabularyRepository.save(new Vocabulary("w", "뜻", saveArticle(alice, "A").getId()));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/vocabularies/memorize").param("ids", v.getId().toString())
                    .with(TestAuth.loginAs(alice)).with(csrf()));
        }

        assertThat(vocabularyRepository.findById(v.getId()).orElseThrow().isMemorized()).isTrue();
    }

    @Test
    void duplicateIdsInOneRequestEndMemorized() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        Vocabulary v = vocabularyRepository.save(new Vocabulary("w", "뜻", saveArticle(alice, "A").getId()));

        mockMvc.perform(post("/vocabularies/memorize")
                .param("ids", v.getId().toString(), v.getId().toString())
                .with(TestAuth.loginAs(alice)).with(csrf()));

        assertThat(vocabularyRepository.findById(v.getId()).orElseThrow().isMemorized()).isTrue();
    }

    private Article saveArticle(User owner, String title) {
        Article article = new Article(title, "https://example.com/" + title, Category.AI, "");
        article.setUserId(owner.getId());
        return articleRepository.save(article);
    }
}
