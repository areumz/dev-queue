package com.areumz.devqueue.repository.memory;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
@Profile("memory")
public class MemoryVocabularyRepository implements VocabularyRepository {
    private final ConcurrentHashMap<Long, Vocabulary> vocabularies = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0L);
    private final MemoryArticleRepository articleRepository;

    public MemoryVocabularyRepository(MemoryArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

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
    public List<Vocabulary> findUnmemorizedByUserId(Long userId) {
        Set<Long> myArticleIds = articleRepository.findByUserId(userId).stream()
                .map(Article::getId)
                .collect(Collectors.toSet());
        return vocabularies.values().stream()
                .filter(vocabulary -> !vocabulary.isMemorized())
                .filter(vocabulary -> myArticleIds.contains(vocabulary.getArticleId()))
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        vocabularies.remove(id);
    }

    @Override
    public void deleteByArticleId(Long articleId) {
        vocabularies.values().removeIf(vocabulary -> vocabulary.getArticleId().equals(articleId));
    }

    @Override
    public void deleteByUserId(Long userId) {
        Set<Long> myArticleIds = articleRepository.findByUserId(userId).stream()
                .map(Article::getId)
                .collect(Collectors.toSet());
        vocabularies.values().removeIf(vocabulary -> myArticleIds.contains(vocabulary.getArticleId()));
    }
}
