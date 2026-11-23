package com.areumz.devqueue.controller;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MyPageForm {

    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;

    private Role role;
    private String roleDetail;

    @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    public static MyPageForm from(User user) {
        MyPageForm form = new MyPageForm();
        form.nickname = user.getNickname();
        form.role = user.getRole();
        form.roleDetail = user.getRoleDetail();
        form.email = user.getEmail();
        return form;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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

    public String getEmail() {
        return email;
    }

    /** SignupForm과 같이 폼에 담기는 순간 정리 */
    public void setEmail(String email) {
        this.email = UserService.normalizeEmail(email);
    }
}
