package com.areumz.devqueue.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areumz.devqueue.support.IntegrationTest;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class CsrfErrorPageTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void forbiddenPageTellsSessionExpired() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("세션이 만료됐어요. 다시 로그인해 주세요")))
                .andExpect(content().string(containsString("/login")));
    }
}
