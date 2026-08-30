package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        model.addAttribute("roles", Role.values());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            return "signup";
        }

        userService.signup(form.getUsername(), form.getPassword(), form.getNickname(), form.getRole(), form.getRoleDetail());
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpServletRequest request, Model model) {
        try {
            User user = userService.login(username, password);

            HttpSession session = request.getSession();
            session.setAttribute("loginUser", user);

            return "redirect:/articles";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if(session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
