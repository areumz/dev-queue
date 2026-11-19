package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.exception.InvalidUrlException;
import com.areumz.devqueue.exception.ResourceNotFoundException;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final VocabularyRepository vocabularyRepository;

    public ArticleService(ArticleRepository articleRepository, VocabularyRepository vocabularyRepository) {
        this.articleRepository = articleRepository;
        this.vocabularyRepository = vocabularyRepository;
    }

    public Article findMyArticle(Long id, Long userId) {
        return articleRepository.findById(id)
                .filter(article -> userId.equals(article.getUserId()))
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public Article create(Long userId, String title, String url, Category category, String memo) {
        String stripped = requireHttpUrl(url);
        Article article = new Article(title, stripped, category, memo);
        article.setUserId(userId);
        return articleRepository.save(article);
    }

    @Transactional
    public void updateMemo(Long id, Long userId, String memo) {
        findMyArticle(id, userId).changeMemo(memo);
    }

    @Transactional
    public void toggleRead(Long id, Long userId) {
        findMyArticle(id, userId).toggleRead();
    }

    public List<Article> findMyArticles(Long userId, Category category) {
        return category == null
                ? articleRepository.findByUserId(userId)
                : articleRepository.findByUserIdAndCategory(userId, category);
    }

    @Transactional
    public void updateArticle(Long id, Long userId, String title, String url, Category category) {
        Article article = findMyArticle(id, userId);
        String stripped = requireHttpUrl(url);
        article.changeTitle(title);
        article.changeUrl(stripped);
        article.changeCategory(category);
    }

    @Transactional
    public void deleteArticle(Long id, Long userId) {
        Long articleId = findMyArticle(id, userId).getId();
        vocabularyRepository.deleteByArticleId(articleId);
        articleRepository.deleteById(articleId);
    }

    private static String requireHttpUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException();
        }
        String url = rawUrl.strip();
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            boolean httpScheme = scheme != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
            if (!httpScheme || uri.getHost() == null) {
                throw new InvalidUrlException();
            }
            return url;
        } catch (URISyntaxException e) {
            throw new InvalidUrlException();
        }
    }
}
