package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;

    @ValidPassword
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;

    private Role role;
    private String roleDetail;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    /** @Email은 앞뒤 공백을 거절하므로, 폼에 담기는 순간 정리한다. */
    public void setEmail(String email) {
        this.email = UserService.normalizeEmail(email);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getRoleDetail() {
        return roleDetail;
    }

    public void setRoleDetail(String roleDetail) {
        this.roleDetail = roleDetail;
    }
}
