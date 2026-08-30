package com.areumz.devqueue.repository;

import com.areumz.devqueue.domain.Vocabulary;

import java.util.List;
import java.util.Optional;

public interface VocabularyRepository {
    Vocabulary save(Vocabulary vocabulary);
    Optional<Vocabulary> findById(Long id);
    List<Vocabulary> findByArticleId(Long articleId);
    void deleteById(Long id);
}
