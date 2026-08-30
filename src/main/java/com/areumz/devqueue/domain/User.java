package com.areumz.devqueue.domain;

import java.time.LocalDate;

public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private LocalDate lastPopupDate;
    private Role role;
    private String roleDetail;

    public User(String username, String password, String nickname,
                Role role, String roleDetail) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.roleDetail = roleDetail;
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

    public LocalDate getLastPopupDate() {
        return lastPopupDate;
    }

    public void updatePopupDate(LocalDate date) {
        this.lastPopupDate = date;
    }
}
