package com.areumz.devqueue.controller;

import com.areumz.devqueue.auth.LoginUser;
import com.areumz.devqueue.auth.LoginUserRefresher;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.DuplicateEmailException;
import com.areumz.devqueue.exception.WrongPasswordException;
import com.areumz.devqueue.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MyPageController {

    private final UserService userService;

    private final LoginUserRefresher loginUserRefresher;

    public MyPageController(UserService userService, LoginUserRefresher loginUserRefresher) {
        this.userService = userService;
        this.loginUserRefresher = loginUserRefresher;
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
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("username", loginUser.getUsername());
        if (bindingResult.hasErrors()) {
            return "mypage";
        }
        User saved;
        try {
            saved = userService.updateProfile(loginUser.getId(), form.getNickname(), form.getRole(),
                    form.getRoleDetail(), form.getEmail());
        } catch (DuplicateEmailException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            return "mypage";
        }
        loginUserRefresher.refresh(saved, request, response);
        redirectAttributes.addFlashAttribute("message", "정보를 저장했어요.");
        return "redirect:/mypage";
    }

    @GetMapping("/mypage/password")
    public String passwordForm(Model model, @AuthenticationPrincipal LoginUser loginUser) {
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        model.addAttribute("loginUser", loginUser);
        return "passwordChange";
    }

    @PostMapping("/mypage/password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm form,
                                 BindingResult bindingResult, Model model,
                                 @AuthenticationPrincipal LoginUser loginUser,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        model.addAttribute("loginUser", loginUser);
        if (bindingResult.hasErrors()) {
            return "passwordChange";
        }
        try {
            userService.changePassword(loginUser.getId(), form.getCurrentPassword(), form.getNewPassword());
        } catch (WrongPasswordException e) {
            bindingResult.rejectValue("currentPassword", "wrong", e.getMessage());
            return "passwordChange";
        }
        // 현재 브라우저의 세션 ID만 새로 발급한다(다른 기기 세션은 유지: 알려진 한계)
        request.changeSessionId();
        redirectAttributes.addFlashAttribute("message", "비밀번호를 바꿨어요.");
        return "redirect:/mypage";
    }


    @GetMapping("/mypage/withdraw")
    public String withdrawForm(Model model, @AuthenticationPrincipal LoginUser loginUser) {
        model.addAttribute("loginUser", loginUser);
        return "withdraw";
    }

    @PostMapping("/mypage/withdraw")
    public String withdraw(@RequestParam(defaultValue = "") String password, Model model,
                           @AuthenticationPrincipal LoginUser loginUser, Authentication authentication,
                           HttpServletRequest request, HttpServletResponse response) {
        try {
            userService.withdraw(loginUser.getId(), password);
        } catch (WrongPasswordException e) {
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("error", e.getMessage());
            return "withdraw";
        }
        new SecurityContextLogoutHandler().logout(request, response, authentication);   // 세션을 끊는다
        return "redirect:/login?withdrawn";   // 세션이 끊겨서 플래시 메시지를 못 쓴다
    }
}
