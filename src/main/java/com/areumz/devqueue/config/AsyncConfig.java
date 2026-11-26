package com.areumz.devqueue.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 메일 처리를 응답과 분리하는 실행기.
 * test 프로필은 호출한 스레드에서 바로 실행하는 동기 실행기를 써서 기존 @Transactional 테스트가 그대로 돈다.
 * async-real 프로필을 같이 켜면 진짜 실행기를 쓴다(비동기 계약 테스트용).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "mailExecutor")
    @Profile("!test | async-real")
    public ThreadPoolTaskExecutor mailExecutor() {
        return newMailExecutor(2, 100);
    }

    @Bean(name = "mailExecutor")
    @Profile("test & !async-real")
    public TaskExecutor syncMailExecutor() {
        return new SyncTaskExecutor();
    }

    /**
     * 스레드 threads개, 대기 큐 queueCapacity칸. 가득 차면 요청을 버리고 경고만 남긴다(메일 폭탄이 서버 자원을 먹지 않게).
     * 초기화하지 않은 채로 돌려준다. 빈으로 쓰면 Spring이 초기화하고, 테스트는 직접 initialize()를 부른다.
     */
    public static ThreadPoolTaskExecutor newMailExecutor(int threads, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler((task, pool) -> log.warn("mail queue is full, request dropped"));
        return executor;
    }
}
