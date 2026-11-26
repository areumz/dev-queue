package com.areumz.devqueue.controller;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.exception.DuplicateEmailException;
import com.areumz.devqueue.exception.InvalidTokenException;
import com.areumz.devqueue.service.EmailVerificationService;
import com.areumz.devqueue.service.EmailVerificationService.SendResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    public EmailVerificationController(EmailVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /** 로그인 없이 열림(메일 앱에서 누르면 로그인이 풀린 브라우저가 열리는 경우가 많음). 토큰이 없어도 같은 안내를 보여줌. */
    @GetMapping("/verify-email")
    public String verify(@RequestParam(defaultValue = "") String token, Model model) {
        try {
            verificationService.verify(token);
            model.addAttribute("title", "이메일 인증이 완료되었어요");
            model.addAttribute("message", "이제 이 이메일로 비밀번호를 되찾을 수 있어요.");
        } catch (InvalidTokenException e) {
            model.addAttribute("title", "링크를 확인해 주세요");
            model.addAttribute("message", e.getMessage());
        } catch (DuplicateEmailException e) {
            model.addAttribute("title", "인증할 수 없어요");
            model.addAttribute("message", e.getMessage());
        }
        return "tokenResult";
    }

    @PostMapping("/mypage/verify-email")
    public String resend(@AuthenticationPrincipal LoginUser loginUser, RedirectAttributes redirectAttributes) {
        SendResult result = verificationService.sendVerification(loginUser.getId());
        redirectAttributes.addFlashAttribute("message", VerificationMessages.of(result));
        return "redirect:/mypage";
    }
}
