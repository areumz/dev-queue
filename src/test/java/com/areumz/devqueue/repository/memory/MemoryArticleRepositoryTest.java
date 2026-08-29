package com.areumz.devqueue.repository.memory;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.repository.ArticleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class MemoryArticleRepositoryTest {
    MemoryArticleRepository repository = new MemoryArticleRepository();

    @AfterEach
    public void afterEach() {
        repository.clearArticles();
    }

    @Test
    public void save() {
        Article article = new Article("제목", "url", Category.DEV_DOC, "memo");
        Article saved = repository.save(article);

        Article result = repository.findById(saved.getId()).get();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    public void findById() {
        Article article = new Article("제목", "url", Category.DEV_DOC, "memo");
        Article saved = repository.save(article);

        Article result = repository.findById(saved.getId()).get();
        assertThat(result).isEqualTo(article);
    }

    @Test
    public void findAll() {
        Article article1 = new Article("제목1", "url1", Category.DEV_DOC, "memo1");
        repository.save(article1);

        Article article2 = new Article("제목2", "url2", Category.DEV_DOC, "memo2");
        repository.save(article2);

        List<Article> result = repository.findAll();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void deleteById() {
        Article article = new Article("제목1", "url1", Category.DEV_DOC, "memo1");
        repository.save(article);

        repository.deleteById(article.getId());

        Optional<Article> result = repository.findById(article.getId());

        assertThat(result.isPresent()).isFalse();
    }
}
