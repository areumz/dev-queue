package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataJpaArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByUserId(Long userId);

    List<Article> findByUserIdAndCategory(Long userId, Category category);
}
