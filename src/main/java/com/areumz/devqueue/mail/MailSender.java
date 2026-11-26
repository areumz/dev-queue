package com.areumz.devqueue.mail;

/** 메일 발송 인터페이스. 구현을 바꿔도(로그, Gmail, SES) 호출부는 한 곳 */
public interface MailSender {

    /** 실패하면 MailDeliveryException을 던짐 */
    void send(String to, String subject, String body);
}
