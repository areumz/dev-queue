package com.areumz.devqueue.controller;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.domain.Article;
import com.areumz.devqueue.domain.Category;
import com.areumz.devqueue.domain.Vocabulary;
import com.areumz.devqueue.exception.InvalidUrlException;
import com.areumz.devqueue.service.ArticleService;
import com.areumz.devqueue.service.VocabularyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final VocabularyService vocabularyService;

    public ArticleController(ArticleService articleService, VocabularyService vocabularyService) {
        this.articleService = articleService;
        this.vocabularyService = vocabularyService;
    }

    @GetMapping("/articles")
    public String list(@RequestParam(required = false) Category category, Model model,
                       @AuthenticationPrincipal LoginUser loginUser) {
        List<Article> articles = articleService.findMyArticles(loginUser.getId(), category);
        model.addAttribute("articles", articles);
        model.addAttribute("category", category);
        model.addAttribute("loginUser", loginUser);

        List<Vocabulary> popupWords = vocabularyService.takeTodayPopupWords(loginUser.getId());
        if(!popupWords.isEmpty()) {
            model.addAttribute("popupWords", popupWords);
        }
        return "articles";
    }

    @GetMapping("/articles/new")
    public String createForm() {
        return "createArticleForm";
    }

    @PostMapping("/articles/new")
    public String create(@RequestParam String title, @RequestParam String url,
                         @RequestParam Category category, @RequestParam String memo,
                         Model model, @AuthenticationPrincipal LoginUser loginUser) {
        try {
            articleService.create(loginUser.getId(), title, url, category, memo);
        } catch (InvalidUrlException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("url", url);
            model.addAttribute("category", category);
            model.addAttribute("memo", memo);
            return "createArticleForm";
        }
        return "redirect:/articles";
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable Long id, Model model,
                         @AuthenticationPrincipal LoginUser loginUser) {
        model.addAttribute("article", articleService.findMyArticle(id, loginUser.getId()));
        model.addAttribute("vocabularies", vocabularyService.findByArticleId(id, loginUser.getId()));
        return "articleDetail";
    }

    @GetMapping("/articles/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal LoginUser loginUser) {
        model.addAttribute("article", articleService.findMyArticle(id, loginUser.getId()));
        return "editArticleForm";
    }

    @PostMapping("/articles/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam String title,
                       @RequestParam String url, Category category,
                       Model model, @AuthenticationPrincipal LoginUser loginUser) {
        try {
            articleService.updateArticle(id, loginUser.getId(), title, url, category);
        } catch (InvalidUrlException e) {
            // article is the stored one (for the id); entered values override it in the form
            model.addAttribute("article", articleService.findMyArticle(id, loginUser.getId()));
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("url", url);
            model.addAttribute("category", category);
            return "editArticleForm";
        }
        return "redirect:/articles/" + id;
    }

    @PostMapping("/articles/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        articleService.deleteArticle(id, loginUser.getId());
        return "redirect:/articles";
    }

    @PostMapping("/articles/{id}/memo")
    public String updateMemo(@PathVariable Long id, @RequestParam String memo,
                             @AuthenticationPrincipal LoginUser loginUser) {
        articleService.updateMemo(id, loginUser.getId(), memo);
        return "redirect:/articles/" + id;
    }

    @PostMapping("/articles/{id}/read")
    public String toggleRead(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        articleService.toggleRead(id, loginUser.getId());
        return "redirect:/articles/" + id;
    }

    @PostMapping("/articles/{id}/vocabularies")
    public String addVocabulary(@PathVariable Long id, @RequestParam String word,
                                @RequestParam String meaning,
                                @AuthenticationPrincipal LoginUser loginUser) {
        vocabularyService.addVocabulary(id, loginUser.getId(), word, meaning);
        return "redirect:/articles/" + id;
    }

    @PostMapping("/vocabularies/{id}/toggle")
    public String toggleVocabulary(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return "redirect:/articles/" + vocabularyService.toggleMemorized(id, loginUser.getId());
    }

    @PostMapping("/vocabularies/{id}/delete")
    public String deleteVocabulary(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return "redirect:/articles/" + vocabularyService.deleteVocabulary(id, loginUser.getId());
    }

    @PostMapping("/vocabularies/memorize")
    public String memorize(@RequestParam(required = false) List<Long> ids,
                           @AuthenticationPrincipal LoginUser loginUser) {
        vocabularyService.memorize(ids, loginUser.getId());
        return "redirect:/articles";
    }
}
