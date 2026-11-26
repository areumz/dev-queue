package com.areumz.devqueue.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncExecutorTest {

    @Test
    void fullQueueDropsExtraRequestsWithoutThrowing() throws Exception {
        ThreadPoolTaskExecutor executor = AsyncConfig.newMailExecutor(1, 1);
        executor.initialize();
        CountDownLatch firstRunning = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger();

        executor.execute(() -> {
            firstRunning.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ran.incrementAndGet();
        });
        assertThat(firstRunning.await(5, TimeUnit.SECONDS)).isTrue();   // 첫 작업이 스레드를 잡고 있다

        executor.execute(ran::incrementAndGet);                          // 대기 큐 1칸을 채운다
        assertThatCode(() -> executor.execute(ran::incrementAndGet)).doesNotThrowAnyException();   // 버려진다
        assertThatCode(() -> executor.execute(ran::incrementAndGet)).doesNotThrowAnyException();   // 버려진다

        release.countDown();
        executor.getThreadPoolExecutor().shutdown();
        assertThat(executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ran.get()).isEqualTo(2);   // 첫 작업 + 큐에 있던 작업. 나머지 둘은 버려졌다
    }
}
