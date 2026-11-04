package com.areumz.devqueue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "redirect:/articles";   // 미로그인이면 인터셉터가 /login으로 보냄
    }
}
