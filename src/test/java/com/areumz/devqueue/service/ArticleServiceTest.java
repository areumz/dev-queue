package com.areumz.devqueue.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.areumz.devqueue.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ArticleServiceTest {

    @Autowired
    ArticleService articleService;

    @Test
    void normalAssembly() {
        assertThat(articleService).isNotNull();
    }
}
