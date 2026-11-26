package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.DuplicateEmailException;
import com.areumz.devqueue.exception.InvalidTokenException;
import com.areumz.devqueue.exception.MailDeliveryException;
import com.areumz.devqueue.mail.MailLinks;
import com.areumz.devqueue.mail.MailSender;
import com.areumz.devqueue.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    public enum SendResult { SENT, SKIPPED, RATE_LIMITED, FAILED }

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final String SUBJECT = "[DevQueue] 이메일 인증";

    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final MailSender mailSender;
    private final MailLinks mailLinks;

    public EmailVerificationService(UserService userService, UserRepository userRepository,
                                    TokenService tokenService, MailSender mailSender, MailLinks mailLinks) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mailSender = mailSender;
        this.mailLinks = mailLinks;
    }

    /**
     * 인증 메일을 보낸다. 이메일이 없거나 이미 인증됐으면 보내지 않는다.
     * 메일 서버가 실패해도 예외를 던지지 않는다(가입·저장은 성공해야 한다).
     * 일부러 @Transactional을 붙이지 않는다: 메일 서버를 기다리는 동안 DB 트랜잭션을 잡고 있지 않기 위해서다.
     */
    public SendResult sendVerification(Long userId) {
        User user = userService.getUser(userId);
        if (user.getEmail() == null || user.isEmailVerified()) {
            return SendResult.SKIPPED;
        }
        Optional<String> rawToken = tokenService.issue(userId, EmailTokenType.VERIFY_EMAIL);
        if (rawToken.isEmpty()) {
            return SendResult.RATE_LIMITED;
        }
        try {
            mailSender.send(user.getEmail(), SUBJECT, body(rawToken.get()));
            return SendResult.SENT;
        } catch (MailDeliveryException e) {
            log.warn("verification mail failed: {}",
                    e.getCause() == null ? "unknown" : e.getCause().getClass().getSimpleName());
            return SendResult.FAILED;
        }
    }

    /**
     * 이메일이 바뀐 뒤에 부른다. 먼저 옛 인증 링크를 무효로 만든다.
     * 새 메일이 횟수 제한에 걸려도 옛 이메일용 링크로 새 이메일을 인증하게 되면 안 되기 때문이다.
     */
    public SendResult sendAfterEmailChange(Long userId) {
        tokenService.invalidateOpen(userId, EmailTokenType.VERIFY_EMAIL);
        return sendVerification(userId);
    }

    /** 링크의 토큰으로 그 사용자의 현재 이메일을 인증한다. 중복 검사는 여기서 한다. */
    @Transactional
    public void verify(String rawToken) {
        EmailToken token = tokenService.findValid(rawToken, EmailTokenType.VERIFY_EMAIL)
                .orElseThrow(InvalidTokenException::new);
        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidTokenException::new);
        String email = user.getEmail();
        if (email == null) {
            throw new InvalidTokenException();
        }
        boolean takenByAnother = userRepository.findVerifiedByEmail(email).stream()
                .anyMatch(owner -> !owner.getId().equals(user.getId()));
        if (takenByAnother) {
            throw new DuplicateEmailException();
        }
        user.verifyEmail();
        tokenService.consume(token);
    }

    private String body(String rawToken) {
        return "DevQueue 이메일 인증\n\n"
                + "아래 링크를 눌러 이메일 인증을 완료해 주세요. 링크는 24시간 동안만 쓸 수 있어요.\n\n"
                + mailLinks.verifyEmail(rawToken) + "\n\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.";
    }
}
