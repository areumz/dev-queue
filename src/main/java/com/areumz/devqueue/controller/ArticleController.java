package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.service.ArticleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    public String list(Model model) {
        List<Article> articles = articleService.findArticles();
        model.addAttribute("articles", articles);
        return "articles";
    }

    @GetMapping("/articles/new")
    public String createForm() {
        return "createArticleForm";
    }

    @PostMapping("/articles/new")
    public String create(@RequestParam String title, @RequestParam String url,
                         @RequestParam Category category, @RequestParam String memo) {
        Article article = new Article(title, url, category, memo);
        articleService.save(article);
        return "redirect:/articles";
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Article article = articleService.findOne(id);
        model.addAttribute("article", article);
        return "articleDetail";
    }

    @PostMapping("/articles/{id}/memo")
    public String updateMemo(@PathVariable Long id, @RequestParam String memo) {
        articleService.updateMemo(id, memo);
        return "redirect:/articles/" + id;
    }

    @PostMapping("/articles/{id}/read")
    public String toggleRead(@PathVariable Long id) {
        articleService.toggleRead(id);
        return "redirect:/articles/" + id;
    }
}
