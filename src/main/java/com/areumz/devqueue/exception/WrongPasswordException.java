package com.areumz.devqueue.exception;

/** The password the user typed does not match the stored one. */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("비밀번호가 올바르지 않습니다.");
    }
}
