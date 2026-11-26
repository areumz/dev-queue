package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.InvalidTokenException;
import com.areumz.devqueue.mail.MailLinks;
import com.areumz.devqueue.mail.MailSender;
import com.areumz.devqueue.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String SUBJECT = "[DevQueue] 비밀번호 재설정";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final MailSender mailSender;
    private final MailLinks mailLinks;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository, TokenService tokenService,
                                MailSender mailSender, MailLinks mailLinks, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mailSender = mailSender;
        this.mailLinks = mailLinks;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 비밀번호 찾기 요청. 계정 조회, 토큰 발급, 횟수 제한 확인, 발송을 통째로 다른 스레드에서 처리한다.
     * 호출한 쪽(컨트롤러)은 기다리지 않고 바로 같은 응답을 준다. 그래야 응답 시간으로 가입 여부를 알 수 없다.
     * 비동기 안의 예외는 호출한 쪽에 전달되지 않으므로 여기서 잡아 클래스 이름만 로그로 남긴다.
     * 토큰과 이메일은 로그에 남기지 않는다.
     */
    // 별도 스레드에서 비동기로 실행 (스레드 2, 대기열 100)
    @Async("mailExecutor") 
    public void requestReset(String rawEmail) {
        try {
            List<User> owners = userRepository.findVerifiedByEmail(rawEmail);
            if (owners.size() != 1) {
                // 0명이면 무시. 2명 이상은 동시에 같은 이메일을 인증한 드문 경우라 누구에게도 보내지 않음
                if (owners.size() > 1) {
                    LOG.warn("password reset skipped: one verified email belongs to {} accounts", owners.size());
                }
                return;
            }
            Optional<String> rawToken = tokenService.issue(owners.get(0).getId(), EmailTokenType.RESET_PASSWORD);
            if (rawToken.isEmpty()) {
                return;
            }
            mailSender.send(rawEmail, SUBJECT, body(rawToken.get()));
        } catch (RuntimeException e) {
            LOG.warn("password reset mail failed: {}", e.getClass().getSimpleName());
        }
    }

    public boolean isTokenValid(String rawToken) {
        return tokenService.findValid(rawToken, EmailTokenType.RESET_PASSWORD).isPresent();
    }

    /** 토큰을 확인하고 비밀번호 변경. 성공하면 그 사용자의 열린 토큰을 모두(방금 쓴 것 포함) 무효로 만듦. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        EmailToken token = tokenService.findValid(rawToken, EmailTokenType.RESET_PASSWORD)
                .orElseThrow(InvalidTokenException::new);
        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidTokenException::new);
        user.changePassword(passwordEncoder.encode(newPassword));
        tokenService.invalidateAllOpen(user.getId());
    }

    private String body(String rawToken) {
        return "DevQueue 비밀번호 재설정\n\n"
                + "아래 링크에서 새 비밀번호를 정해 주세요. 링크는 30분 동안만 쓸 수 있어요.\n\n"
                + mailLinks.resetPassword(rawToken) + "\n\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해 주세요. 비밀번호는 바뀌지 않아요.";
    }
}
