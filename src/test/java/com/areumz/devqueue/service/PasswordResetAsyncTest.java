package com.areumz.devqueue.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.areumz.devqueue.config.AsyncConfig;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.mail.MailLinks;
import com.areumz.devqueue.mail.MailSender;
import com.areumz.devqueue.repository.EmailTokenRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.memory.MemoryEmailTokenRepository;
import com.areumz.devqueue.repository.memory.MemoryUserRepository;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 찾기 요청이 메일 발송이 끝나기를 기다리지 않는다는 것을 증명한다.
 * 시간을 재지 않고 latch로 순서를 고정한다: 가짜 MailSender가 발송 중에 멈춰 서 있고,
 * 그동안 요청은 이미 돌아왔는지 확인한 뒤 멈춤을 풀어 메일이 도착하는지 본다.
 */
class PasswordResetAsyncTest {

    static class BlockingMailSender implements MailSender {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        final List<String> sent = new CopyOnWriteArrayList<>();
        volatile String threadName;

        @Override
        public void send(String to, String subject, String body) {
            threadName = Thread.currentThread().getName();
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("not released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            sent.add(to);
            done.countDown();
        }
    }

    @Test
    void requestReturnsBeforeTheMailIsSentAndTheMailArrivesAfterRelease() {
        BlockingMailSender mail = new BlockingMailSender();

        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("async-real"))
                .withUserConfiguration(AsyncConfig.class)
                .withBean(UserRepository.class, MemoryUserRepository::new)
                .withBean(EmailTokenRepository.class, MemoryEmailTokenRepository::new)
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withBean(TokenService.class)
                .withBean(MailSender.class, () -> mail)
                .withBean(MailLinks.class, () -> new MailLinks("https://devqueue.test"))
                .withBean(PasswordEncoder.class, BCryptPasswordEncoder::new)
                .withBean(PasswordResetService.class)
                .run(context -> {
                    User alice = new User("alice", "x", "앨리스", Role.BACKEND, null, "a@x.com");
                    alice.verifyEmail();
                    context.getBean(UserRepository.class).save(alice);

                    context.getBean(PasswordResetService.class).requestReset("a@x.com");

                    // 이 줄에 왔다는 것은 요청이 이미 돌아왔다는 뜻이다. 그런데 발송은 아직 끝나지 않았다.
                    assertThat(mail.entered.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThat(mail.sent).isEmpty();
                    assertThat(mail.threadName).startsWith("mail-");

                    mail.release.countDown();
                    assertThat(mail.done.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThat(mail.sent).containsExactly("a@x.com");
                });
    }
}
