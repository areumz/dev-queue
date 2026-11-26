package com.areumz.devqueue.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 로컬·테스트용: 진짜로 보내지 않고 내용을 로그에 찍는다. 링크(토큰)가 로그에 남으므로 운영에서 쓰면 안 된다. */
public class LogMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LogMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[LogMailSender] to={} subject={}\n{}", to, subject, body);
    }
}
