package com.areumz.devqueue.exception;

/** Another account already uses this email. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("이미 사용 중인 이메일입니다.");
    }
}
