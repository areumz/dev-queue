package com.areumz.devqueue.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.domain.EmailToken;
import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.EmailTokenRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.jpa.SpringDataJpaEmailTokenRepository;
import com.areumz.devqueue.service.EmailVerificationService;
import com.areumz.devqueue.service.TokenService;
import com.areumz.devqueue.service.UserService;
import com.areumz.devqueue.support.IntegrationTest;
import com.areumz.devqueue.support.RecordingMailSender;
import com.areumz.devqueue.support.TestAuth;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class EmailVerificationTest {

    private static final String INVALID = "올바르지 않거나 만료";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmailVerificationService verificationService;

    @Autowired
    TokenService tokenService;

    @Autowired
    EmailTokenRepository tokenRepository;

    @Autowired
    SpringDataJpaEmailTokenRepository tokenRows;

    @Autowired
    RecordingMailSender mailSender;

    @Autowired
    Clock clock;

    @Autowired
    EntityManager em;

    User alice;

    @BeforeEach
    void setUp() {
        mailSender.clear();
        alice = userService.signup("alice", "password123", "앨리스", Role.BACKEND, null, "a@x.com");
    }

    private ResultActions signup(String username, String email) throws Exception {
        return mockMvc.perform(post("/signup").with(csrf())
                .param("username", username).param("password", "password123")
                .param("nickname", "닉").param("role", "BACKEND").param("email", email));
    }

    private ResultActions saveProfile(User who, String email) throws Exception {
        return mockMvc.perform(post("/mypage").with(TestAuth.loginAs(who)).with(csrf())
                .param("nickname", "앨리스").param("role", "BACKEND").param("email", email));
    }

    private ResultActions verifyLink(String token) throws Exception {
        return mockMvc.perform(get("/verify-email").param("token", token));
    }

    private String lastToken() {
        List<RecordingMailSender.Sent> sent = mailSender.sent();
        return RecordingMailSender.tokenOf(sent.get(sent.size() - 1));
    }

    private boolean isVerified(User user) {
        em.flush();
        em.clear();
        return userRepository.findById(user.getId()).orElseThrow().isEmailVerified();
    }

    // ---- 가입 ----

    @Test
    void signupWithEmailSendsAVerificationMail() throws Exception {
        signup("carol", " Carol@X.com ").andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", containsString("인증 메일을 보냈어요")));

        assertThat(mailSender.sent()).hasSize(1);
        RecordingMailSender.Sent mail = mailSender.sent().get(0);
        assertThat(mail.to()).isEqualTo("carol@x.com");
        assertThat(mail.body()).contains("https://devqueue.test/verify-email?token=");
        assertThat(userRepository.findByUsername("carol").orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void signupWithoutEmailSendsNothing() throws Exception {
        signup("carol", "").andExpect(redirectedUrl("/login"));
        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void signupStillSucceedsWhenTheMailFails() throws Exception {
        mailSender.setFailing(true);

        signup("carol", "carol@x.com").andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", containsString("보내지 못했어요")));

        assertThat(userRepository.findByUsername("carol")).isPresent();
    }

    // ---- 링크 ----

    @Test
    void linkVerifiesTheEmailEvenWithoutLogin() throws Exception {
        verificationService.sendVerification(alice.getId());

        verifyLink(lastToken()).andExpect(status().isOk())
                .andExpect(content().string(containsString("인증이 완료")));

        assertThat(isVerified(alice)).isTrue();
    }

    @Test
    void linkWorksOnlyOnce() throws Exception {
        verificationService.sendVerification(alice.getId());
        String token = lastToken();

        verifyLink(token).andExpect(content().string(containsString("인증이 완료")));
        verifyLink(token).andExpect(content().string(containsString(INVALID)));
    }

    @Test
    void profileUpdateKillsTheOldLinkInTheSameStepAsTheEmailChange() throws Exception {
        verificationService.sendVerification(alice.getId());
        String oldToken = lastToken();

        userService.updateProfile(alice.getId(), "앨리스", Role.BACKEND, null, "new@x.com");

        verifyLink(oldToken).andExpect(content().string(containsString(INVALID)));
        assertThat(isVerified(alice)).isFalse();
    }

    @Test
    void unknownExpiredAndUsedLinksLookTheSame() throws Exception {
        String unknown = verifyLink("no-such-token").andReturn().getResponse().getContentAsString();

        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.save(new EmailToken(alice.getId(), EmailTokenType.VERIFY_EMAIL,
                TokenService.hash("expired-raw"), now.minusMinutes(1), now.minusHours(25)));
        String expired = verifyLink("expired-raw").andReturn().getResponse().getContentAsString();

        verificationService.sendVerification(alice.getId());
        String used = lastToken();
        verifyLink(used);
        String afterUse = verifyLink(used).andReturn().getResponse().getContentAsString();

        assertThat(unknown).contains(INVALID);
        assertThat(expired).isEqualTo(unknown);
        assertThat(afterUse).isEqualTo(unknown);
    }

    @Test
    void openingTheLinkWithoutATokenShowsTheSamePage() throws Exception {
        mockMvc.perform(get("/verify-email")).andExpect(status().isOk())
                .andExpect(content().string(containsString(INVALID)));
    }

    // ---- 이메일 변경 ----

    @Test
    void changingTheEmailMakesItUnverifiedAndSendsAMailToTheNewAddress() throws Exception {
        alice.verifyEmail();

        saveProfile(alice, "New@X.com").andExpect(redirectedUrl("/mypage"))
                .andExpect(flash().attribute("message", containsString("인증 메일을 보냈어요")));

        assertThat(isVerified(alice)).isFalse();
        assertThat(mailSender.sent()).hasSize(1);
        assertThat(mailSender.sent().get(0).to()).isEqualTo("new@x.com");
    }

    @Test
    void savingTheSameEmailOrOnlyTheNicknameKeepsVerificationAndSendsNothing() throws Exception {
        alice.verifyEmail();

        saveProfile(alice, "A@X.com").andExpect(redirectedUrl("/mypage"));

        assertThat(mailSender.sent()).isEmpty();
        assertThat(isVerified(alice)).isTrue();
    }

    @Test
    void oldLinkStopsWorkingAfterTheEmailChanges() throws Exception {
        verificationService.sendVerification(alice.getId());
        String oldToken = lastToken();

        saveProfile(alice, "new@x.com");

        verifyLink(oldToken).andExpect(content().string(containsString(INVALID)));
        assertThat(isVerified(alice)).isFalse();
    }

    /** 제한에 걸려 새 메일이 못 가도, 옛 이메일용 링크로 새 이메일을 인증하게 되면 안 된다. */
    @Test
    void oldLinkIsDeadEvenWhenTheNewMailIsRateLimited() throws Exception {
        verificationService.sendVerification(alice.getId());
        String oldToken = lastToken();
        LocalDateTime now = LocalDateTime.now(clock);
        for (int i = 0; i < 2; i++) {   // 최근 1시간에 총 3개가 되어 다음 발급은 막힌다
            EmailToken filler = new EmailToken(alice.getId(), EmailTokenType.VERIFY_EMAIL,
                    TokenService.hash("filler" + i), now.plusHours(1), now);
            filler.markUsed(now);
            tokenRepository.save(filler);
        }

        saveProfile(alice, "new@x.com").andExpect(redirectedUrl("/mypage"))
                .andExpect(flash().attribute("message", containsString("너무 자주")));

        verifyLink(oldToken).andExpect(content().string(containsString(INVALID)));
        assertThat(isVerified(alice)).isFalse();
    }

    @Test
    void removingTheEmailKillsOpenLinks() throws Exception {
        verificationService.sendVerification(alice.getId());
        String token = lastToken();

        saveProfile(alice, "").andExpect(redirectedUrl("/mypage"));

        verifyLink(token).andExpect(content().string(containsString(INVALID)));
    }

    // ---- 중복 ----

    @Test
    void unverifiedEmailCanBeSharedAndOnlyTheFirstToVerifyKeepsIt() throws Exception {
        User bob = userService.signup("bob", "password123", "밥", Role.BACKEND, null, "A@x.com");
        verificationService.sendVerification(alice.getId());
        String aliceToken = lastToken();
        verificationService.sendVerification(bob.getId());
        String bobToken = lastToken();

        verifyLink(aliceToken).andExpect(content().string(containsString("인증이 완료")));
        verifyLink(bobToken).andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 다른 계정에서 인증된 이메일입니다.")));

        assertThat(isVerified(alice)).isTrue();
        assertThat(isVerified(bob)).isFalse();
    }

    // ---- 재발송 ----

    @Test
    void resendSendsAMailForAnUnverifiedEmail() throws Exception {
        mockMvc.perform(post("/mypage/verify-email").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(redirectedUrl("/mypage"))
                .andExpect(flash().attribute("message", containsString("인증 메일을 보냈어요")));

        assertThat(mailSender.sent()).hasSize(1);
    }

    @Test
    void resendIsLimitedToThreePerHour() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/mypage/verify-email").with(TestAuth.loginAs(alice)).with(csrf()))
                    .andExpect(flash().attribute("message", containsString("인증 메일을 보냈어요")));
        }

        mockMvc.perform(post("/mypage/verify-email").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(flash().attribute("message", containsString("너무 자주")));

        assertThat(mailSender.sent()).hasSize(3);
    }

    @Test
    void resendDoesNothingForAVerifiedEmailOrNoEmail() throws Exception {
        alice.verifyEmail();
        User noEmail = userService.signup("dave", "password123", "데이브", Role.BACKEND, null, null);

        mockMvc.perform(post("/mypage/verify-email").with(TestAuth.loginAs(alice)).with(csrf()))
                .andExpect(flash().attribute("message", containsString("이미 인증")));
        mockMvc.perform(post("/mypage/verify-email").with(TestAuth.loginAs(noEmail)).with(csrf()))
                .andExpect(flash().attribute("message", containsString("이미 인증")));

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void resendWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/mypage/verify-email").with(TestAuth.loginAs(alice)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousResendGoesToLogin() throws Exception {
        mockMvc.perform(post("/mypage/verify-email").with(csrf())).andExpect(redirectedUrl("/login"));
    }

    // ---- 화면 ----

    @Test
    void mypageShowsUnverifiedStatusWithAResendButton() throws Exception {
        mockMvc.perform(get("/mypage").with(TestAuth.loginAs(alice)))
                .andExpect(content().string(containsString("미인증")))
                .andExpect(content().string(containsString("인증 메일 다시 보내기")));
    }

    @Test
    void mypageShowsVerifiedStatusWithoutAResendButton() throws Exception {
        alice.verifyEmail();

        mockMvc.perform(get("/mypage").with(TestAuth.loginAs(alice)))
                .andExpect(content().string(containsString("인증됨")))
                .andExpect(content().string(not(containsString("미인증"))))
                .andExpect(content().string(not(containsString("인증 메일 다시 보내기"))));
    }

    // ---- 탈퇴 ----

    @Test
    void withdrawalAlsoDeletesTheUsersTokens() throws Exception {
        User bob = userService.signup("bob", "password123", "밥", Role.BACKEND, null, "b@x.com");
        verificationService.sendVerification(alice.getId());
        verificationService.sendVerification(bob.getId());

        mockMvc.perform(post("/mypage/withdraw").with(TestAuth.loginAs(alice)).with(csrf())
                .param("password", "password123")).andExpect(redirectedUrl("/login?withdrawn"));
        em.flush();
        em.clear();

        assertThat(tokenRows.findAll()).extracting(EmailToken::getUserId).containsExactly(bob.getId());
    }
}
