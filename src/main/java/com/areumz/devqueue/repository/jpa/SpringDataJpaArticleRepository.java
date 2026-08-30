package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaArticleRepository extends JpaRepository<Article, Long> {
}
