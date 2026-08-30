package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataJpaVocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByArticleId(Long articleId);
}
