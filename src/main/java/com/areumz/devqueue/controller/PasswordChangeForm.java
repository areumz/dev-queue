package com.areumz.devqueue.controller;

import com.areumz.devqueue.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public class PasswordChangeForm {

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;

    @ValidPassword
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
