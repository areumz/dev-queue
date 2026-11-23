package com.areumz.devqueue.repository;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository {
    Article save(Article article);
    Optional<Article> findById(Long id);
    List<Article> findAll();
    List<Article> findByUserId(Long userId);
    List<Article> findByUserIdAndCategory(Long userId, Category category);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
}
