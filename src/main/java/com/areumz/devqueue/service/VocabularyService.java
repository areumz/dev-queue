package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.exception.ResourceNotFoundException;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VocabularyService {
    private final VocabularyRepository vocabularyRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public VocabularyService(VocabularyRepository vocabularyRepository,
                             ArticleRepository articleRepository,
                             UserRepository userRepository,
                             Clock clock) {
        this.vocabularyRepository = vocabularyRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public Vocabulary addVocabulary(Long articleId, Long userId, String word, String meaning) {
        findMyArticle(articleId, userId);
        return vocabularyRepository.save(new Vocabulary(word, meaning, articleId));
    }

    public List<Vocabulary> findByArticleId(Long articleId, Long userId) {
        findMyArticle(articleId, userId);
        return vocabularyRepository.findByArticleId(articleId);
    }

    @Transactional
    public Long toggleMemorized(Long id, Long userId) {
        Vocabulary vocabulary = findMyVocabulary(id, userId);
        vocabulary.toggleMemorized();
        return vocabulary.getArticleId();
    }

    @Transactional
    public Long deleteVocabulary(Long id, Long userId) {
        Vocabulary vocabulary = findMyVocabulary(id, userId);
        vocabularyRepository.deleteById(id);
        return vocabulary.getArticleId();
    }

    @Transactional
    public void memorize(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Vocabulary> mine = ids.stream().map(id -> findMyVocabulary(id, userId)).toList();
        mine.forEach(Vocabulary::markMemorized);
    }

    private Article findMyArticle(Long articleId, Long userId) {
        return articleRepository.findById(articleId)
                .filter(article -> article.getUserId().equals(userId))
                .orElseThrow(ResourceNotFoundException::new);
    }

    private Vocabulary findMyVocabulary(Long id, Long userId) {
        Vocabulary vocabulary = vocabularyRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        findMyArticle(vocabulary.getArticleId(), userId);
        return vocabulary;
    }

    public List<Vocabulary> findUnmemorizedVocabulariesByUserId(Long userId) {
        return new ArrayList<>(vocabularyRepository.findUnmemorizedByUserId(userId));
    }

    public List<Vocabulary> getRandomUnmemorized(Long userId) {
        List<Vocabulary> unmemorized = findUnmemorizedVocabulariesByUserId(userId);
        Collections.shuffle(unmemorized);
        int count = Math.min(3, unmemorized.size());
        return new ArrayList<>(unmemorized.subList(0, count));
    }

    @Transactional
    public List<Vocabulary> takeTodayPopupWords(Long userId) {
        LocalDate today = LocalDate.now(clock);
        User user = userRepository.findById(userId).orElseThrow();
        if (today.equals(user.getLastPopupDate())) {
            return List.of();
        }
        List<Vocabulary> words = getRandomUnmemorized(userId);
        if (!words.isEmpty()) {
            user.updatePopupDate(today);
        }
        return words;
    }
}
