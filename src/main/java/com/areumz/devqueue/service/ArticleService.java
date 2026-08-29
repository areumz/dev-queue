package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
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

    public Article findOne(Long id) {
        return articleRepository.findById(id).orElseThrow();
    }

    public void updateMemo(Long id, String memo) {
        Article article = articleRepository.findById(id).orElseThrow();
        article.changeMemo(memo);
    }

    public void toggleRead(Long id) {
        Article article = articleRepository.findById(id).orElseThrow();
        article.toggleRead();
    }

    public List<Article> findByCategory(Category category) {
        return articleRepository.findAll().stream()
                .filter(article -> article.getCategory() == category)
                .toList();
    }
}
