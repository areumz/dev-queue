package com.areumz.devqueue.exception;

/** Another account already uses this username. */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("이미 존재하는 아이디입니다.");
    }
}
