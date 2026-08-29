package com.areumz.devqueue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ArticleController {

    @GetMapping("/articles")
    public String list(Model model) {
        List<String> articles = new ArrayList<>();

        articles.add("article 1");
        articles.add("article 2");
        articles.add("article 3");

        model.addAttribute("articles", articles);

        return "articles";
    }
}
