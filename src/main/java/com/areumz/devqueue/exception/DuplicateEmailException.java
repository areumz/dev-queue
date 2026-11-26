package com.areumz.devqueue.exception;

/** Another account already uses this email. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("이미 다른 계정에서 인증된 이메일입니다.");
    }
}
