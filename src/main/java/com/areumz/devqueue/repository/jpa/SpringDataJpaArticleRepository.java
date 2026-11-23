package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataJpaArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByUserId(Long userId);

    List<Article> findByUserIdAndCategory(Long userId, Category category);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Article a where a.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
