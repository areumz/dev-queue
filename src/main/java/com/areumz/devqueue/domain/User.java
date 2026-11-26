package com.areumz.devqueue.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private LocalDate lastPopupDate;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String roleDetail;

    private String email;

    private boolean emailVerified;

    protected User() {
    }

    public User(String username, String password, String nickname,
                Role role, String roleDetail) {
        this(username, password, nickname, role, roleDetail, null);
    }

    public User(String username, String password, String nickname,
                Role role, String roleDetail, String email) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.roleDetail = roleDetail;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public Role getRole() {
        return role;
    }

    public String getRoleDetail() {
        return roleDetail;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public LocalDate getLastPopupDate() {
        return lastPopupDate;
    }

    public void updatePopupDate(LocalDate date) {
        this.lastPopupDate = date;
    }

    /** 이메일이 바뀌었으면 true return. 그때는 인증 상태도 되돌림(새 이메일은 아직 인증 전 상태). */
    public boolean updateProfile(String nickname, Role role, String roleDetail, String email) {
        boolean emailChanged = !Objects.equals(this.email, email);
        this.nickname = nickname;
        this.role = role;
        this.roleDetail = roleDetail;
        this.email = email;
        if (emailChanged) {
            this.emailVerified = false;
        }
        return emailChanged;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
