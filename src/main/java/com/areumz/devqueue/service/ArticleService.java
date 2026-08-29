package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Article save(Article article) {
        return articleRepository.save(article);
    }

    public List<Article> findArticles() {
        return articleRepository.findAll();
    }
}
