package com.areumz.devqueue.controller;

import com.areumz.devqueue.exception.InvalidTokenException;
import com.areumz.devqueue.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        return "forgotPassword";
    }

    /** 처리는 다른 스레드에서 하고 바로 같은 화면을 보여준다. 이메일을 화면에 되돌려 보여주지 않는다. */
    @PostMapping("/forgot-password")
    public String forgot(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "forgotPassword";
        }
        passwordResetService.requestReset(form.getEmail());
        return "forgotPasswordSent";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(defaultValue = "") String token, Model model) {
        if (!passwordResetService.isTokenValid(token)) {
            return invalidLink(model);
        }
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "resetPassword";
    }

    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                        BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "resetPassword";   // 토큰은 숨은 필드에 그대로 남아 있다
        }
        try {
            passwordResetService.resetPassword(form.getToken(), form.getNewPassword());
        } catch (InvalidTokenException e) {
            return invalidLink(model);
        }
        return "redirect:/login?reset";
    }

    private String invalidLink(Model model) {
        model.addAttribute("title", "링크를 확인해 주세요");
        model.addAttribute("message", new InvalidTokenException().getMessage());
        return "tokenResult";
    }
}
