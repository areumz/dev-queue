package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaVocabularyRepository implements VocabularyRepository {

    private final SpringDataJpaVocabularyRepository springDataJpaVocabularyRepository;

    public JpaVocabularyRepository(SpringDataJpaVocabularyRepository springDataJpaVocabularyRepository) {
        this.springDataJpaVocabularyRepository = springDataJpaVocabularyRepository;
    }

    @Override
    public Vocabulary save(Vocabulary vocabulary) {
        return springDataJpaVocabularyRepository.save(vocabulary);
    }

    @Override
    public Optional<Vocabulary> findById(Long id) {
        return springDataJpaVocabularyRepository.findById(id);
    }

    @Override
    public List<Vocabulary> findByArticleId(Long articleId) {
        return springDataJpaVocabularyRepository.findByArticleId(articleId);
    }

    @Override
    public void deleteById(Long id) {
        springDataJpaVocabularyRepository.deleteById(id);
    }
}
