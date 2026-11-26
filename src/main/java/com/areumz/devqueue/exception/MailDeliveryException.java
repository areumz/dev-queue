package com.areumz.devqueue.exception;

/** 메일 서버가 메일을 받아주지 않았다. 원인은 getCause()에 있다. */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(Throwable cause) {
        super("메일을 보내지 못했어요.", cause);
    }
}
