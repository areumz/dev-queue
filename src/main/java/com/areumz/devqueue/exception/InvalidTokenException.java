package com.areumz.devqueue.exception;

/** 토큰이 없거나, 만료됐거나, 이미 썼다. 이유는 일부러 구분하지 않는다. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("링크가 올바르지 않거나 만료되었어요. 다시 요청해 주세요.");
    }
}
