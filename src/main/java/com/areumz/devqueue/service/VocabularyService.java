package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VocabularyService {
    private final VocabularyRepository vocabularyRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
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
}
