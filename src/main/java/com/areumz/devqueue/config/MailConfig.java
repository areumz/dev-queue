package com.areumz.devqueue.config;

import com.areumz.devqueue.mail.LogMailSender;
import com.areumz.devqueue.mail.MailSender;
import com.areumz.devqueue.mail.SmtpMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 발송기 선택. (app.mail.enabled, app.base-url은 기본값 x)
 * 없거나 잘못되면 앱이 시작되지 않음: 운영에서 설정을 빠뜨렸을 때 로그 발송으로 돌아가
 * 재설정 링크(토큰)가 로그에 남는 일을 막기 위함
 */
@Configuration
public class MailConfig {

    @Bean
    public MailSender appMailSender(@Value("${app.mail.enabled}") String enabled,
                                    @Value("${app.base-url}") String baseUrl,
                                    ObjectProvider<JavaMailSender> javaMailSender) {
        // boolean으로 받으면 yes, on, 1도 참이 된다. 오타를 막으려고 문자열로 받아 정확히 비교한다.
        if ("false".equals(enabled)) {
            return new LogMailSender();
        }
        if (!"true".equals(enabled)) {
            throw new IllegalStateException(
                    "app.mail.enabled는 true 또는 false로 적어야 합니다. 현재 값: " + enabled);
        }
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "app.mail.enabled=true이면 app.base-url은 https://로 시작해야 합니다.");
        }
        JavaMailSender sender = javaMailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException(
                    "app.mail.enabled=true이면 spring.mail.host 등 SMTP 설정이 필요합니다.");
        }
        return new SmtpMailSender(sender);
    }
}
