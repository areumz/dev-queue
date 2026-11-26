package com.areumz.devqueue.controller;

import com.areumz.devqueue.service.EmailVerificationService.SendResult;

/** 인증 메일 발송 결과를 사용자 문구로 바꾼다. */
final class VerificationMessages {

    private VerificationMessages() {
    }

    static String of(SendResult result) {
        return switch (result) {
            case SENT -> "인증 메일을 보냈어요. 스팸함도 확인해 주세요.";
            case SKIPPED -> "인증할 이메일이 없거나 이미 인증됐어요.";
            case RATE_LIMITED -> "인증 메일을 너무 자주 요청했어요. 1시간 뒤에 다시 시도해 주세요.";
            case FAILED -> "인증 메일을 보내지 못했어요. 마이페이지에서 다시 보내 주세요.";
        };
    }
}
