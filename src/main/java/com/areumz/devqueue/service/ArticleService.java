package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void updateMemo(Long id, String memo) {
        Article article = articleRepository.findById(id).orElseThrow();
        article.changeMemo(memo);
    }

    @Transactional
    public void toggleRead(Long id) {
        Article article = articleRepository.findById(id).orElseThrow();
        article.toggleRead();
    }

    public List<Article> findByCategory(Category category) {
        return articleRepository.findAll().stream()
                .filter(article -> article.getCategory() == category)
                .toList();
    }

    public List<Article> findMyArticles(Long userId, Category category) {
        return articleRepository.findAll().stream()
                .filter(article->article.getUserId().equals(userId))
                .filter(article->category==null || article.getCategory() == category )
                .toList();
    }

    @Transactional
    public void updateArticle(Long id, String title, String url, Category category) {
        Article article = articleRepository.findById(id).orElseThrow();
        article.changeTitle(title);
        article.changeUrl(url);
        article.changeCategory(category);
    }

    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }
}
