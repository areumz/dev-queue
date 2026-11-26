package com.areumz.devqueue.mail;

/** 메일 발송을 숨기는 인터페이스. 구현을 바꿔도(로그, Gmail, SES) 호출하는 쪽은 그대로다. */
public interface MailSender {

    /** 실패하면 MailDeliveryException을 던진다. */
    void send(String to, String subject, String body);
}
