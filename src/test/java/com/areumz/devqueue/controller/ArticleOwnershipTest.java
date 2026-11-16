package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.exception.ResourceNotFoundException;
import com.areumz.devqueue.repository.VocabularyRepository;
import com.areumz.devqueue.service.VocabularyService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleOwnershipTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    @Autowired
    VocabularyService vocabularyService;

    @Autowired
    EntityManager entityManager;

    User alice;
    User bob;
    Article article;
    Vocabulary vocab;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));
        article = new Article("원래 제목", "https://example.com", Category.AI, "원래 메모");
        article.setUserId(alice.getId());
        articleRepository.save(article);
        vocab = vocabularyRepository.save(new Vocabulary("idempotent", "멱등", article.getId()));
    }

    private Article reloadArticle() {
        entityManager.flush();
        entityManager.clear();
        return articleRepository.findById(article.getId()).orElseThrow();
    }

    private Vocabulary reloadVocab(Long id) {
        entityManager.flush();
        entityManager.clear();
        return vocabularyRepository.findById(id).orElseThrow();
    }

    @Test
    void bobCannotViewAliceArticle() throws Exception {
        mockMvc.perform(get("/articles/" + article.getId()).with(TestAuth.loginAs(bob)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bobCannotOpenEditForm() throws Exception {
        mockMvc.perform(get("/articles/" + article.getId() + "/edit").with(TestAuth.loginAs(bob)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bobCannotEditAliceArticle() throws Exception {
        mockMvc.perform(post("/articles/" + article.getId() + "/edit")
                        .param("title", "해킹").param("url", "https://evil.com").param("category", "AI")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reloadArticle().getTitle()).isEqualTo("원래 제목");
    }

    @Test
    void bobCannotDeleteAliceArticle() throws Exception {
        mockMvc.perform(post("/articles/" + article.getId() + "/delete")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(articleRepository.findById(article.getId())).isPresent();
    }

    @Test
    void bobCannotChangeAliceMemo() throws Exception {
        mockMvc.perform(post("/articles/" + article.getId() + "/memo").param("memo", "해킹")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reloadArticle().getMemo()).isEqualTo("원래 메모");
    }

    @Test
    void bobCannotToggleAliceRead() throws Exception {
        mockMvc.perform(post("/articles/" + article.getId() + "/read")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reloadArticle().isRead()).isFalse();
    }

    @Test
    void bobCannotAddWordToAliceArticle() throws Exception {
        mockMvc.perform(post("/articles/" + article.getId() + "/vocabularies")
                        .param("word", "w").param("meaning", "m")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        entityManager.flush();
        entityManager.clear();
        assertThat(vocabularyRepository.findByArticleId(article.getId())).hasSize(1);
    }

    @Test
    void bobCannotToggleAliceWord() throws Exception {
        mockMvc.perform(post("/vocabularies/" + vocab.getId() + "/toggle")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reloadVocab(vocab.getId()).isMemorized()).isFalse();
    }

    @Test
    void bobCannotDeleteAliceWord() throws Exception {
        mockMvc.perform(post("/vocabularies/" + vocab.getId() + "/delete")
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(vocabularyRepository.findById(vocab.getId())).isPresent();
    }

    @Test
    void memorizeRejectsWholeRequestIfAnyIdIsNotMine() throws Exception {
        Article bobArticle = new Article("밥 글", "https://example.com", Category.AI, "");
        bobArticle.setUserId(bob.getId());
        articleRepository.save(bobArticle);
        Vocabulary bobVocab = vocabularyRepository.save(new Vocabulary("mine", "내 것", bobArticle.getId()));

        mockMvc.perform(post("/vocabularies/memorize")
                        .param("ids", bobVocab.getId().toString(), vocab.getId().toString())
                        .with(TestAuth.loginAs(bob)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reloadVocab(bobVocab.getId()).isMemorized()).isFalse();
        assertThat(reloadVocab(vocab.getId()).isMemorized()).isFalse();
    }

    @Test
    void unknownArticleIdIsNotFound() throws Exception {
        mockMvc.perform(get("/articles/999999").with(TestAuth.loginAs(alice)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanEditDeleteAndManageWords() throws Exception {
        String base = "/articles/" + article.getId();
        mockMvc.perform(post(base + "/edit")
                        .param("title", "새 제목").param("url", "https://example.org").param("category", "AI")
                        .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(status().isFound());
        assertThat(reloadArticle().getTitle()).isEqualTo("새 제목");

        mockMvc.perform(post(base + "/vocabularies").param("word", "w").param("meaning", "m")
                        .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl(base));

        mockMvc.perform(post("/vocabularies/" + vocab.getId() + "/toggle")
                        .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl(base));
        assertThat(reloadVocab(vocab.getId()).isMemorized()).isTrue();

        mockMvc.perform(post("/vocabularies/" + vocab.getId() + "/delete")
                        .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl(base));
        entityManager.flush();
        entityManager.clear();
        assertThat(vocabularyRepository.findById(vocab.getId())).isEmpty();

        mockMvc.perform(post(base + "/memo").param("memo", "새 메모")
                        .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl(base));
        assertThat(reloadArticle().getMemo()).isEqualTo("새 메모");

        mockMvc.perform(post(base + "/read").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl(base));
        assertThat(reloadArticle().isRead()).isTrue();

        mockMvc.perform(post(base + "/delete").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl("/articles"));
        entityManager.flush();
        entityManager.clear();
        assertThat(articleRepository.findById(article.getId())).isEmpty();
    }

    @Test
    void wordActionsRedirectToWordsOwnArticle() throws Exception {
        mockMvc.perform(post("/vocabularies/" + vocab.getId() + "/toggle").param("articleId", "999")
                        .with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl("/articles/" + article.getId()));
    }

    @Test
    void memorizeWithNoIdsJustRedirects() throws Exception {
        mockMvc.perform(post("/vocabularies/memorize").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl("/articles"));
    }

    @Test
    void vocabularyServiceRefusesListingOthersArticle() {
        assertThatThrownBy(() -> vocabularyService.findByArticleId(article.getId(), bob.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
