package com.areumz.devqueue.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 메일에 넣을 링크를 만든다. 주소는 app.base-url 설정(기본값 없음)에서 온다. */
@Component
public class MailLinks {

    private final String baseUrl;

    public MailLinks(@Value("${app.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String verifyEmail(String rawToken) {
        return baseUrl + "/verify-email?token=" + rawToken;
    }

    public String resetPassword(String rawToken) {
        return baseUrl + "/reset-password?token=" + rawToken;
    }
}
