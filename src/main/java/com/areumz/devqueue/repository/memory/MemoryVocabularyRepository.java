package com.areumz.devqueue.repository.memory;

import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MemoryVocabularyRepository implements VocabularyRepository {
    private final ConcurrentHashMap<Long, Vocabulary> vocabularies = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0L);

    @Override
    public Vocabulary save(Vocabulary vocabulary) {
        vocabulary.setId(sequence.incrementAndGet());
        vocabularies.put(vocabulary.getId(), vocabulary);
        return vocabulary;
    }

    @Override
    public Optional<Vocabulary> findById(Long id) {
        return Optional.ofNullable(vocabularies.get(id));
    }

    @Override
    public List<Vocabulary> findByArticleId(Long articleId) {
        return vocabularies.values().stream()
                .filter(vocabulary -> vocabulary.getArticleId().equals(articleId))
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        vocabularies.remove(id);
    }
}
