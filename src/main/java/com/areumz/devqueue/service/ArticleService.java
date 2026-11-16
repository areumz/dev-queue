package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.exception.ResourceNotFoundException;
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

    public Article findMyArticle(Long id, Long userId) {
        return articleRepository.findById(id)
                .filter(article -> article.getUserId().equals(userId))
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public Article create(Long userId, String title, String url, Category category, String memo) {
        Article article = new Article(title, url, category, memo);
        article.setUserId(userId);
        return articleRepository.save(article);
    }

    public List<Article> findArticles() {
        return articleRepository.findAll();
    }

    @Transactional
    public void updateMemo(Long id, Long userId, String memo) {
        findMyArticle(id, userId).changeMemo(memo);
    }

    @Transactional
    public void toggleRead(Long id, Long userId) {
        findMyArticle(id, userId).toggleRead();
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
    public void updateArticle(Long id, Long userId, String title, String url, Category category) {
        Article article = findMyArticle(id, userId);
        article.changeTitle(title);
        article.changeUrl(url);
        article.changeCategory(category);
    }

    @Transactional
    public void deleteArticle(Long id, Long userId) {
        articleRepository.deleteById(findMyArticle(id, userId).getId());
    }
}
