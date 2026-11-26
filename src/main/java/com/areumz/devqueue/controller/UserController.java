package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.DuplicateUsernameException;
import com.areumz.devqueue.service.EmailVerificationService;
import com.areumz.devqueue.service.EmailVerificationService.SendResult;
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
    private final EmailVerificationService emailVerificationService;

    public UserController(UserService userService, EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
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

        User saved;
        try {
            saved = userService.signup(form.getUsername(), form.getPassword(), form.getNickname(),
                    form.getRole(), form.getRoleDetail(), form.getEmail());
        } catch (DuplicateUsernameException e) {
            bindingResult.rejectValue("username", "duplicate", e.getMessage());
            return "signup";
        }
        // 메일이 실패해도 가입은 이미 성공. 결과를 안내 문구에 덧붙인다.
        SendResult sent = emailVerificationService.sendVerification(saved.getId());
        String message = "회원가입이 완료되었습니다.";
        if (sent != SendResult.SKIPPED) {
            message += " " + VerificationMessages.of(sent);
        }
        redirectAttributes.addFlashAttribute("message", message + " 로그인해주세요!");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}
