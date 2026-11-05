package org.example.knockin.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.example.knockin.KnockInApplication;
import org.example.knockin.alarm.service.impl.FcmServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FcmServiceAsyncTest.AsyncTestConfig.class)
@DisplayName("FCM 비동기 서비스")
class FcmServiceAsyncTest {

    @Autowired
    private FcmServiceImpl fcmService;

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("애플리케이션에서 비동기 처리를 활성화한다")
    void applicationEnablesAsyncProcessing() {
        assertThat(AnnotatedElementUtils.hasAnnotation(KnockInApplication.class, EnableAsync.class)).isTrue();
    }

    @Test
    @DisplayName("Firebase 전송은 호출 스레드와 다른 스레드에서 실행한다")
    void sendRunsOnAnotherThread() throws Exception {
        // given
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> firebaseThread = new AtomicReference<>();
        CountDownLatch firebaseCalled = new CountDownLatch(1);
        given(firebaseMessaging.send(any(Message.class))).willAnswer(invocation -> {
            firebaseThread.set(Thread.currentThread());
            firebaseCalled.countDown();
            return "message-id";
        });

        // when
        fcmService.send("제목", "본문", "fcm-token", "knockinrn://chat/10");

        // then
        assertThat(firebaseCalled.await(3, SECONDS)).isTrue();
        assertThat(firebaseThread.get()).isNotSameAs(callerThread);
    }

    @Configuration
    @EnableAsync
    @Import(FcmServiceImpl.class)
    static class AsyncTestConfig {

        @Bean
        FirebaseMessaging firebaseMessaging() {
            return mock(FirebaseMessaging.class);
        }

        @Bean
        ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(10);
            executor.setThreadNamePrefix("fcm-test-");
            executor.initialize();
            return executor;
        }
    }
}
