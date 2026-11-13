package org.example.knockin.global.auth.service;

import feign.Client;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.knockin.global.auth.client.KakaoUnlinkFeignClient;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.verification.entity.LoginProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.aop.support.AopUtils;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class KakaoUnlinkCircuitBreakerTest {
    private static final String BREAKER = "kakaoUnlink";
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class, CircuitBreakerAutoConfiguration.class,
                    AopAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("kakao.admin.key=test-only");

    @Test
    void opensOnFailuresSkipsRemoteCallAndRecoversAfterSuccessfulProbes(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(KakaoOauth2DeleteService.class);
            var transport = context.getBean(TestTransport.class);
            var breaker = context.getBean(CircuitBreakerRegistry.class).circuitBreaker(BREAKER);

            var unlinkService = context.getBean(Oauth2DeleteFactory.class).getDeleteService(LoginProviderType.KAKAO);
            assertThat(unlinkService).isSameAs(service);
            for (int i = 0; i < 5; i++) assertThat(unlinkService.requestUnlink("123")).isFalse();
            assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(transport.calls.get()).isEqualTo(5);
            assertThat(service.requestUnlink("123")).isFalse();
            assertThat(transport.calls.get()).isEqualTo(5);
            assertThat(transport.options.connectTimeoutMillis()).isEqualTo(1000);
            assertThat(transport.options.readTimeoutMillis()).isEqualTo(2000);

            // Exercise recovery without waiting for the production 30-second open interval.
            breaker.transitionToHalfOpenState();
            transport.status = 200;
            for (int i = 0; i < 3; i++) assertThat(service.requestUnlink("123")).isTrue();
            assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(transport.calls.get()).isEqualTo(8);
            assertThat(output).contains("HTTP status: 503", "서킷이 열려 있습니다.");
        });
    }

    @Test
    void directFeignCallUsesFallbackForFailureAndOpenCircuit(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var client = context.getBean(KakaoUnlinkFeignClient.class);
            assertThat(AopUtils.isAopProxy(client)).isTrue();
            assertThat(AopUtils.isAopProxy(context.getBean(KakaoOauth2DeleteService.class))).isFalse();
            var form = Map.<String, Object>of("target_id_type", "user_id", "target_id", "123");

            assertThat(client.unlink("KakaoAK test-only", form)).isNull();
            var breaker = context.getBean(CircuitBreakerRegistry.class).circuitBreaker(BREAKER);
            assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
            breaker.transitionToOpenState();
            assertThat(client.unlink("KakaoAK test-only", form)).isNull();

            assertThat(context.getBean(TestTransport.class).calls.get()).isEqualTo(1);
            assertThat(output).contains("HTTP status: 503", "서킷이 열려 있습니다.");
        });
    }

    @Test
    void stillDeletesMemberWhenCircuitIsOpen() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(KakaoOauth2DeleteService.class);
            context.getBean(CircuitBreakerRegistry.class).circuitBreaker(BREAKER).transitionToOpenState();
            Member member = Member.builder().id(1L).providerId("123").providerType(LoginProviderType.KAKAO).build();
            var repository = mock(MemberRepository.class);
            when(repository.findMemberByProvider("123", LoginProviderType.KAKAO)).thenReturn(Optional.of(member));
            var members = new MemberServiceImpl(repository, context.getBean(Oauth2DeleteFactory.class),
                    null, null, null, null, null, null, null, null, null, null);

            assertThat(members.deleteMember("123", LoginProviderType.KAKAO).getUpdatedAt()).isNotNull();
            assertThat(member.isDelete()).isTrue();
            assertThat(context.getBean(TestTransport.class).calls.get()).isZero();
        });
    }

    @Test
    void returnsFalseAndRecordsSocketTimeout() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var transport = context.getBean(TestTransport.class);
            transport.timeout = true;

            assertThat(context.getBean(KakaoOauth2DeleteService.class).requestUnlink("123")).isFalse();

            assertThat(transport.calls.get()).isEqualTo(1);
            assertThat(context.getBean(CircuitBreakerRegistry.class).circuitBreaker(BREAKER)
                    .getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 204})
    void returnsFalseForEmptySuccessfulHttpResponse(int status) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var transport = context.getBean(TestTransport.class);
            transport.status = status;
            transport.body = "";

            assertThat(context.getBean(Oauth2DeleteFactory.class).getDeleteService(LoginProviderType.KAKAO)
                    .requestUnlink("123")).isFalse();

            assertThat(transport.calls.get()).isEqualTo(1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableFeignClients(clients = KakaoUnlinkFeignClient.class)
    @Import({KakaoOauth2DeleteService.class, Oauth2DeleteFactory.class})
    @PropertySource("classpath:application.properties")
    static class TestConfiguration {
        @Bean
        TestTransport transport() { return new TestTransport(); }
    }

    static class TestTransport implements Client {
        final AtomicInteger calls = new AtomicInteger();
        volatile int status = 503;
        volatile String body = "{\"id\":123}";
        volatile boolean timeout;
        volatile Request.Options options;

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            calls.incrementAndGet();
            this.options = options;
            if (timeout) {
                throw new SocketTimeoutException("test read timeout");
            }
            return Response.builder().request(request).status(status).reason("test")
                    .headers(Map.of("Content-Type", List.of("application/json")))
                    .body(body, StandardCharsets.UTF_8).build();
        }
    }
}
