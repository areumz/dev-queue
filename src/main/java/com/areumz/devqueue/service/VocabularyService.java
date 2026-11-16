package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VocabularyService {
    private final VocabularyRepository vocabularyRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository,
                             ArticleRepository articleRepository,
                             UserRepository userRepository) {
        this.vocabularyRepository = vocabularyRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public Vocabulary addVocabulary(String word, String meaning, Long articleId) {
        Vocabulary vocabulary = new Vocabulary(word, meaning, articleId);
        return vocabularyRepository.save(vocabulary);
    }

    public List<Vocabulary> findByArticleId(Long articleId) {
        return vocabularyRepository.findByArticleId(articleId);
    }

    @Transactional
    public void toggleMemorized(Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id).orElseThrow();
        vocabulary.toggleMemorized();
    }

    @Transactional
    public void deleteVocabulary(Long id) {
        vocabularyRepository.deleteById(id);
    }

    public List<Vocabulary> findUnmemorizedVocabulariesByUserId(Long userId) {
        List<Article> myArticles = articleRepository.findAll().stream()
                .filter(article -> article.getUserId().equals(userId))
                .toList();

        List<Vocabulary> unmemorized = new ArrayList<>();

        for(Article article : myArticles){
            List<Vocabulary> vocabularies = vocabularyRepository.findByArticleId(article.getId());
            for(Vocabulary vocabulary : vocabularies){
                if(!vocabulary.isMemorized()){
                    unmemorized.add(vocabulary);
                }
            }
        }
        return unmemorized;
    }

    public List<Vocabulary> getRandomUnmemorized(Long userId) {
        List<Vocabulary> unmemorized = findUnmemorizedVocabulariesByUserId(userId);
        Collections.shuffle(unmemorized);
        int count = Math.min(3, unmemorized.size());
        return new ArrayList<>(unmemorized.subList(0, count));
    }

    @Transactional
    public List<Vocabulary> takeTodayPopupWords(Long userId) {
        LocalDate today = LocalDate.now();
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
