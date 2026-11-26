package com.areumz.devqueue.controller;

import com.areumz.devqueue.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordForm {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    public String getEmail() {
        return email;
    }

    /** SignupForm과 같이 폼에 담기는 순간 정리 */
    public void setEmail(String email) {
        this.email = UserService.normalizeEmail(email);
    }
}
