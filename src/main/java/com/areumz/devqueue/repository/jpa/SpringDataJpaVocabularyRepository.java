package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataJpaVocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByArticleId(Long articleId);

    void deleteByArticleId(Long articleId);

    @Query("select v from Vocabulary v where v.memorized = false "
            + "and v.articleId in (select a.id from Article a where a.userId = :userId)")
    List<Vocabulary> findUnmemorizedByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Vocabulary v where v.articleId in "
            + "(select a.id from Article a where a.userId = :userId)")
    void deleteByUserId(@Param("userId") Long userId);
}
