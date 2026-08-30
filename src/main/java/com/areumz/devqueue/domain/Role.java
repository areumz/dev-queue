package com.areumz.devqueue.domain;

public enum Role {
    FRONTEND("프론트엔드 개발자"),
    BACKEND("백엔드 개발자"),
    APP("앱 개발자"),
    WEB("웹 개발자"),
    FULLSTACK("풀스택 개발자"),
    DATA("데이터 엔지니어"),
    PUBLISHER("웹 퍼블리셔"),
    ETC("기타");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
