package com.areumz.devqueue.mail;

import com.areumz.devqueue.exception.MailDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** 운영용: spring.mail.* 설정의 SMTP 서버(Gmail 등)로 텍스트 메일 발송. */
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;

    public SmtpMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            throw new MailDeliveryException(e);
        }
    }
}
