package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VocabularyService {
    private final VocabularyRepository vocabularyRepository;
    private final ArticleRepository articleRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository,
                             ArticleRepository articleRepository) {
        this.vocabularyRepository = vocabularyRepository;
        this.articleRepository = articleRepository;
    }

    public Vocabulary addVocabulary(String word, String meaning, Long articleId) {
        Vocabulary vocabulary = new Vocabulary(word, meaning, articleId);
        return vocabularyRepository.save(vocabulary);
    }

    public List<Vocabulary> findByArticleId(Long articleId) {
        return vocabularyRepository.findByArticleId(articleId);
    }

    public void toggleMemorized(Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id).orElseThrow();
        vocabulary.toggleMemorized();
    }

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
}
