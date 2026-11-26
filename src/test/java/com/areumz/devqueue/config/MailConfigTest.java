package com.areumz.devqueue.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.areumz.devqueue.mail.LogMailSender;
import com.areumz.devqueue.mail.MailSender;
import com.areumz.devqueue.mail.SmtpMailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MailConfig.class)
            .withPropertyValues("app.base-url=https://devqueue.test");

    private static String rootMessage(AssertableApplicationContext context) {
        return NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure()).getMessage();
    }

    @Test
    void missingEnabledPreventsStartup() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootMessage(context)).contains("app.mail.enabled");
        });
    }

    @Test
    void valueOtherThanTrueOrFalseIsRejected() {
        runner.withPropertyValues("app.mail.enabled=yes").run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootMessage(context)).contains("app.mail.enabled");
        });
    }

    @Test
    void falseUsesTheLogSender() {
        runner.withPropertyValues("app.mail.enabled=false").run(context ->
                assertThat(context.getBean(MailSender.class)).isInstanceOf(LogMailSender.class));
    }

    @Test
    void trueUsesTheSmtpSender() {
        runner.withPropertyValues("app.mail.enabled=true")
                .withBean(JavaMailSender.class, JavaMailSenderImpl::new)
                .run(context -> assertThat(context.getBean(MailSender.class)).isInstanceOf(SmtpMailSender.class));
    }

    @Test
    void trueWithHttpBaseUrlIsRejected() {
        runner.withPropertyValues("app.mail.enabled=true", "app.base-url=http://devqueue.test")
                .withBean(JavaMailSender.class, JavaMailSenderImpl::new)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootMessage(context)).contains("https://");
                });
    }

    @Test
    void trueWithoutSmtpSettingsIsRejected() {
        runner.withPropertyValues("app.mail.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootMessage(context)).contains("spring.mail.host");
        });
    }
}
