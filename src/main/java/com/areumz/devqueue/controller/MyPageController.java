package com.areumz.devqueue.controller;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.DuplicateEmailException;
import com.areumz.devqueue.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MyPageController {

    private final UserService userService;

    public MyPageController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("roles")
    public Role[] roles() {
        return Role.values();
    }

    @GetMapping("/mypage")
    public String form(Model model, @AuthenticationPrincipal LoginUser loginUser) {
        User user = userService.getUser(loginUser.getId());
        model.addAttribute("myPageForm", MyPageForm.from(user));
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("username", user.getUsername());
        return "mypage";
    }

    @PostMapping("/mypage")
    public String update(@Valid @ModelAttribute("myPageForm") MyPageForm form,
                         BindingResult bindingResult, Model model,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("username", loginUser.getUsername());
        if (bindingResult.hasErrors()) {
            return "mypage";
        }
        try {
            userService.updateProfile(loginUser.getId(), form.getNickname(), form.getRole(),
                    form.getRoleDetail(), form.getEmail());
        } catch (DuplicateEmailException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            return "mypage";
        }
        redirectAttributes.addFlashAttribute("message", "정보를 저장했어요.");
        return "redirect:/mypage";
    }
}
