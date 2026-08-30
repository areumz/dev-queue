package com.areumz.devqueue.domain;

import java.time.LocalDate;

public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private LocalDate lastPopupDate;

    public User(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
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
