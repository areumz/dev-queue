package com.areumz.devqueue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import com.areumz.devqueue.support.IntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class DailyPopupTimezoneTest {

    @MockitoBean
    Clock clock;

    @Autowired
    VocabularyService vocabularyService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VocabularyRepository vocabularyRepository;

    @Test
    void dayChangesAtKoreanMidnight() {
        given(clock.instant()).willReturn(Instant.parse("2026-11-14T15:30:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
        User alice = new User("alice", "x", "앨리스", Role.BACKEND, null);
        alice.updatePopupDate(LocalDate.of(2026, 11, 14));
        userRepository.save(alice);
        Article a = new Article("글", "https://example.com", Category.AI, "");
        a.setUserId(alice.getId());
        articleRepository.save(a);
        vocabularyRepository.save(new Vocabulary("idempotent", "멱등", a.getId()));

        List<Vocabulary> words = vocabularyService.takeTodayPopupWords(alice.getId());

        assertThat(words).hasSize(1);
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getLastPopupDate())
                .isEqualTo(LocalDate.of(2026, 11, 15));
    }
}
