package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.repository.ArticleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaArticleRepository implements ArticleRepository {

    private final SpringDataJpaArticleRepository springDataJpaArticleRepository;

    public JpaArticleRepository(SpringDataJpaArticleRepository springDataJpaArticleRepository) {
        this.springDataJpaArticleRepository = springDataJpaArticleRepository;
    }

    @Override
    public Article save(Article article) {
        return springDataJpaArticleRepository.save(article);
    }

    @Override
    public Optional<Article> findById(Long id) {
        return springDataJpaArticleRepository.findById(id);
    }

    @Override
    public List<Article> findAll() {
        return springDataJpaArticleRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        springDataJpaArticleRepository.deleteById(id);
    }
}
