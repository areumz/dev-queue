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
import com.areumz.devqueue.service.VocabularyService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class DailyPopupTest {

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

    @Test
    void popupShowsOncePerDay() throws Exception {
        User alice = userRepository.save(new User("alice", "x", "앨리스", Role.BACKEND, null));
        Article a = new Article("글", "https://example.com", Category.AI, "");
        a.setUserId(alice.getId());
        articleRepository.save(a);
        vocabularyRepository.save(new Vocabulary("idempotent", "멱등", a.getId()));

        mockMvc.perform(get("/articles").with(TestAuth.loginAs(alice)))
                .andExpect(model().attributeExists("popupWords"));
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getLastPopupDate()).isNotNull();

        mockMvc.perform(get("/articles").with(TestAuth.loginAs(alice)))
                .andExpect(model().attributeDoesNotExist("popupWords"));
    }

    @Test
    void takeTodayPopupWordsIsEmptyWithoutWordsAndKeepsDateUnset() {
        User bob = userRepository.save(new User("bob", "x", "밥", Role.BACKEND, null));

        assertThat(vocabularyService.takeTodayPopupWords(bob.getId())).isEmpty();
        assertThat(userRepository.findById(bob.getId()).orElseThrow().getLastPopupDate()).isNull();
    }
}
