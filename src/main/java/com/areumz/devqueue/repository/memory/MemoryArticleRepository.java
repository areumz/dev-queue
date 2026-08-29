package com.areumz.devqueue.repository.memory;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.repository.ArticleRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MemoryArticleRepository implements ArticleRepository {
    private final ConcurrentHashMap<Long, Article> articles = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong(0L);

    @Override
    public Article save(Article article) {
        article.setId(sequence.incrementAndGet());
        articles.put(article.getId(), article);
        return article;
    }

    @Override
    public Optional<Article> findById(Long id) {
        return Optional.ofNullable(articles.get(id));
    }

    @Override
    public List<Article> findAll() {
        return new ArrayList<>(articles.values());
    }

    @Override
    public void deleteById(Long id) {
        articles.remove(id);
    }

    public void clearArticles() {
        articles.clear();
    }
}
