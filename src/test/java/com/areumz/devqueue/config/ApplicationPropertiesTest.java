package com.areumz.devqueue.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApplicationPropertiesTest {

    /** 기본값이 있으면 운영에서 설정을 빠뜨려도 조용히 로그 발송으로 돌아간다. */
    @Test
    void requiredMailSettingsHaveNoDefaultInMainProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(in);
        }
        assertThat(properties)
                .doesNotContainKey("app.mail.enabled")
                .doesNotContainKey("app.base-url");
    }
}
