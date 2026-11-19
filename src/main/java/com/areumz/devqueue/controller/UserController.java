package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("roles")
    public Role[] roles() {
        return Role.values();
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form,
                         BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if(bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            userService.signup(form.getUsername(), form.getPassword(), form.getNickname(), form.getRole(), form.getRoleDetail());
        } catch (IllegalStateException e) {
            bindingResult.rejectValue("username", "duplicate", e.getMessage());
            return "signup";
        }
        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요!");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}
