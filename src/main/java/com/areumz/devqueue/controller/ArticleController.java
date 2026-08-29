package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    public String list(@RequestParam(required = false) Category category, Model model,
                       HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User loginUser = (User) session.getAttribute("loginUser");

        List<Article> articles = articleService.findMyArticles(loginUser.getId(), category);
        model.addAttribute("articles", articles);
        return "articles";
    }

    @GetMapping("/articles/new")
    public String createForm() {
        return "createArticleForm";
    }

    @PostMapping("/articles/new")
    public String create(@RequestParam String title, @RequestParam String url,
                         @RequestParam Category category, @RequestParam String memo,
                         HttpServletRequest request) {
        Article article = new Article(title, url, category, memo);
        HttpSession session = request.getSession(false);
        User loginUser = (User) session.getAttribute("loginUser");
        article.setUserId(loginUser.getId());

        articleService.save(article);
        return "redirect:/articles";
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable Long id, Model model,
                         HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User loginUser = (User) session.getAttribute("loginUser");

        Article article = articleService.findOne(id);

        if(!article.getUserId().equals(loginUser.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
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
