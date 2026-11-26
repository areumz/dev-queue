package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.EmailTokenRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.service.PasswordResetService;
import com.areumz.devqueue.service.TokenService;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.RecordingMailSender;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class PasswordResetTest {

    private static final String INVALID = "올바르지 않거나 만료";
    private static final String NEW_PASSWORD = "brandnew123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TokenService tokenService;

    @Autowired
    UserService userService;

    @Autowired
    EmailTokenRepository tokenRepository;

    @Autowired
    RecordingMailSender mailSender;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    Clock clock;

    @Autowired
    EntityManager em;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        mailSender.clear();
        alice = new User("alice", passwordEncoder.encode("oldpassword1"), "앨리스", Role.BACKEND, null, "a@x.com");
        alice.verifyEmail();
        alice = userRepository.save(alice);
        bob = userRepository.save(
                new User("bob", passwordEncoder.encode("oldpassword2"), "밥", Role.BACKEND, null, "b@x.com"));
    }

    private ResultActions forgot(String email) throws Exception {
        return mockMvc.perform(post("/forgot-password").with(csrf()).param("email", email));
    }

    private ResultActions reset(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/reset-password").with(csrf())
                .param("token", token).param("newPassword", newPassword));
    }

    private String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private String resetTokenFor(User user) {
        return tokenService.issue(user.getId(), EmailTokenType.RESET_PASSWORD).orElseThrow();
    }

    private boolean passwordIs(User user, String raw) {
        em.flush();
        em.clear();
        return passwordEncoder.matches(raw, userRepository.findById(user.getId()).orElseThrow().getPassword());
    }

    // ---- 요청 ----

    @Test
    void verifiedEmailGetsAMailWithAResetLink() throws Exception {
        forgot(" A@x.com ").andExpect(status().isOk())
                .andExpect(content().string(containsString("안내 메일을 보냈습니다")));

        assertThat(mailSender.sent()).hasSize(1);
        assertThat(mailSender.sent().get(0).to()).isEqualTo("a@x.com");
        assertThat(mailSender.sent().get(0).body()).contains("https://devqueue.test/reset-password?token=");
    }

    @Test
    void unknownAndUnverifiedEmailsGetNoMailButTheSameResponse() throws Exception {
        String registered = body(forgot("a@x.com"));
        String unknown = body(forgot("nobody@x.com"));
        String unverified = body(forgot("b@x.com"));

        assertThat(unknown).isEqualTo(registered);
        assertThat(unverified).isEqualTo(registered);
        assertThat(mailSender.sent()).hasSize(1);   // 인증된 a@x.com 하나뿐
    }

    @Test
    void twoVerifiedOwnersOfTheSameEmailGetNoMail() throws Exception {
        User other = new User("alice2", passwordEncoder.encode("oldpassword3"), "앨리스2", Role.BACKEND, null, "a@x.com");
        other.verifyEmail();
        userRepository.save(other);

        forgot("a@x.com").andExpect(status().isOk());

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void blankAndMalformedEmailsShowFormErrors() throws Exception {
        forgot("").andExpect(status().isOk())
                .andExpect(content().string(containsString("이메일을 입력해주세요.")));
        forgot("not-an-email").andExpect(status().isOk())
                .andExpect(content().string(containsString("이메일 형식이 올바르지 않습니다.")));
        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void fourthRequestInAnHourSendsNothingButLooksTheSame() throws Exception {
        String first = body(forgot("a@x.com"));
        forgot("a@x.com");
        forgot("a@x.com");
        String fourth = body(forgot("a@x.com"));

        assertThat(mailSender.sent()).hasSize(3);
        assertThat(fourth).isEqualTo(first);
    }

    @Test
    void whenTheMailFailsTheResponseIsTheSameAndNoTokenOrEmailIsLogged() throws Exception {
        String ok = body(forgot("a@x.com"));
        mailSender.clear();
        mailSender.setFailing(true);

        Logger logger = (Logger) LoggerFactory.getLogger(PasswordResetService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String failed = body(forgot("a@x.com").andExpect(status().isOk()));
            assertThat(failed).isEqualTo(ok);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(mailSender.sent()).isEmpty();
        assertThat(mailSender.attempted()).hasSize(1);
        String token = RecordingMailSender.tokenOf(mailSender.attempted().get(0));
        List<String> logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(logged).isNotEmpty();
        assertThat(logged).allSatisfy(line -> assertThat(line).doesNotContain(token).doesNotContain("a@x.com"));
    }

    // ---- 링크와 재설정 ----

    @Test
    void validLinkShowsTheFormAndInvalidLinkShowsTheNotice() throws Exception {
        String token = resetTokenFor(alice);

        mockMvc.perform(get("/reset-password").param("token", token)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"token\"")))
                .andExpect(content().string(containsString("name=\"newPassword\"")));
        mockMvc.perform(get("/reset-password").param("token", "bogus")).andExpect(status().isOk())
                .andExpect(content().string(containsString(INVALID)));
    }

    @Test
    void resetChangesThePasswordAndSendsTheUserToLogin() throws Exception {
        String token = resetTokenFor(alice);

        reset(token, NEW_PASSWORD).andExpect(redirectedUrl("/login?reset"));

        assertThat(passwordIs(alice, NEW_PASSWORD)).isTrue();
        assertThat(passwordIs(alice, "oldpassword1")).isFalse();
    }

    @Test
    void theSameLinkCannotBeUsedTwice() throws Exception {
        String token = resetTokenFor(alice);
        reset(token, NEW_PASSWORD).andExpect(redirectedUrl("/login?reset"));

        reset(token, "another12345").andExpect(status().isOk())
                .andExpect(content().string(containsString(INVALID)));
        assertThat(passwordIs(alice, NEW_PASSWORD)).isTrue();
    }

    @Test
    void expiredLinkIsRejected() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.save(new EmailToken(alice.getId(), EmailTokenType.RESET_PASSWORD,
                TokenService.hash("expired-raw"), now.minusMinutes(1), now.minusMinutes(31)));

        reset("expired-raw", NEW_PASSWORD).andExpect(content().string(containsString(INVALID)));
        assertThat(passwordIs(alice, "oldpassword1")).isTrue();
    }

    @Test
    void weakPasswordIsRejectedAndTheLinkStaysUsable() throws Exception {
        String token = resetTokenFor(alice);

        reset(token, "short").andExpect(status().isOk())
                .andExpect(content().string(containsString("8자 이상")))
                .andExpect(content().string(containsString(token)));   // 폼을 다시 보여줄 때 토큰을 유지한다
        assertThat(passwordIs(alice, "oldpassword1")).isTrue();

        reset(token, NEW_PASSWORD).andExpect(redirectedUrl("/login?reset"));
    }

    @Test
    void successInvalidatesTheOtherOpenTokensOfTheUser() throws Exception {
        String verifyToken = tokenService.issue(alice.getId(), EmailTokenType.VERIFY_EMAIL).orElseThrow();
        String token = resetTokenFor(alice);

        reset(token, NEW_PASSWORD).andExpect(redirectedUrl("/login?reset"));

        mockMvc.perform(get("/verify-email").param("token", verifyToken))
                .andExpect(content().string(containsString(INVALID)));
    }

    @Test
    void aVerificationTokenCannotResetAPassword() throws Exception {
        String verifyToken = tokenService.issue(alice.getId(), EmailTokenType.VERIFY_EMAIL).orElseThrow();

        reset(verifyToken, NEW_PASSWORD).andExpect(content().string(containsString(INVALID)));
        assertThat(passwordIs(alice, "oldpassword1")).isTrue();
    }

    @Test
    void aTokenOfADeletedUserIsInvalid() throws Exception {
        String token = resetTokenFor(alice);
        userRepository.deleteById(alice.getId());
        em.flush();
        em.clear();

        reset(token, NEW_PASSWORD).andExpect(status().isOk())
                .andExpect(content().string(containsString(INVALID)));
    }

    @Test
    void unknownExpiredAndUsedLinksLookTheSame() throws Exception {
        String unknown = body(mockMvc.perform(get("/reset-password").param("token", "bogus")));

        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.save(new EmailToken(alice.getId(), EmailTokenType.RESET_PASSWORD,
                TokenService.hash("expired-raw"), now.minusMinutes(1), now.minusMinutes(31)));
        String expired = body(mockMvc.perform(get("/reset-password").param("token", "expired-raw")));

        String used = resetTokenFor(alice);
        reset(used, NEW_PASSWORD);
        String afterUse = body(mockMvc.perform(get("/reset-password").param("token", used)));

        assertThat(unknown).contains(INVALID);
        assertThat(expired).isEqualTo(unknown);
        assertThat(afterUse).isEqualTo(unknown);
    }

    // ---- 다른 경로가 열린 재설정 링크를 죽인다 ----

    @Test
    void changingThePasswordKillsOpenResetLinks() throws Exception {
        String token = resetTokenFor(alice);

        userService.changePassword(alice.getId(), "oldpassword1", "changed12345");

        assertThat(tokenService.findValid(token, EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void changingTheEmailKillsOpenResetLinks() throws Exception {
        String token = resetTokenFor(alice);

        userService.updateProfile(alice.getId(), "앨리스", Role.BACKEND, null, "new@x.com");

        assertThat(tokenService.findValid(token, EmailTokenType.RESET_PASSWORD)).isEmpty();
    }

    // ---- 보안 ----

    @Test
    void postsWithoutCsrfAreForbidden() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "a@x.com")).andExpect(status().isForbidden());
        mockMvc.perform(post("/reset-password").param("token", "x").param("newPassword", NEW_PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void newPathsAreOpenAndOthersStillNeedLogin() throws Exception {
        mockMvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mockMvc.perform(get("/reset-password")).andExpect(status().isOk());
        mockMvc.perform(get("/verify-email")).andExpect(status().isOk());
        mockMvc.perform(get("/mypage")).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/articles")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageHasTheForgotLinkAndTheResetNotice() throws Exception {
        mockMvc.perform(get("/login")).andExpect(content().string(containsString("/forgot-password")))
                .andExpect(content().string(containsString("비밀번호를 잊으셨나요?")));
        mockMvc.perform(get("/login").param("reset", "")).andExpect(content().string(containsString("비밀번호를 바꿨어요")));
    }
}
