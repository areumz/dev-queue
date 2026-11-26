package com.areumz.devqueue.controller;

import com.areumz.devqueue.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public class ResetPasswordForm {

    private String token;

    @ValidPassword
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    private String newPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
