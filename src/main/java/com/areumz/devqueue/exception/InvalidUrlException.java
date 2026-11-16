package com.areumz.devqueue.exception;

/** The URL is not an http/https address with a host. */
public class InvalidUrlException extends RuntimeException {
    public InvalidUrlException() {
        super("http 또는 https 주소만 저장할 수 있어요");
    }
}
