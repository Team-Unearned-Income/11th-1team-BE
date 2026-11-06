package org.example.knockin.verification.service.impl;

import java.util.Optional;
import javax.sql.DataSource;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.AuthenticateAlarmTemplate;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationApprove;
import org.example.knockin.verification.repository.AuthenticationApproveRepository;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.example.knockin.verification.service.AuthenticationService;
import org.example.knockin.verification.service.MailSendService;
import org.example.knockin.verification.service.VerificationDeleteEvent;
import org.example.knockin.verification.service.VerificationSaveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(VerificationEventIntegrationTest.TestConfig.class)
@DisplayName("인증 승인·반려 이벤트의 트랜잭션 처리 테스트")
class VerificationEventIntegrationTest {

    @Autowired private AuthenticationService authenticationService;
    @Autowired private AuthenticationApproveRepository authenticationApproveRepository;
    @Autowired private MemberServiceImpl memberService;
    @Autowired private AlarmServiceImpl alarmService;
    @Autowired private PushNotificationServiceImpl pushNotificationService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetMocks() {
        reset(authenticationApproveRepository, memberService, alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("인증 승인 트랜잭션이 커밋된 뒤에만 회원에게 알림을 전송한다")
    void sendsApprovalNotificationsOnlyAfterTransactionCommits() {
        AuthenticationApprove approve = pendingApproval();
        Member member = Member.builder().id(10L).build();
        given(authenticationApproveRepository.findById(approve.getId())).willReturn(Optional.of(approve));
        given(memberService.findByIdOrThrow(member.getId())).willReturn(member);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            authenticationService.saveVerifications(approve.getId());

            assertThat(approve.getStatus()).isEqualTo(ApproveType.ACCEPTED);
            verifyNoInteractions(memberService, alarmService, pushNotificationService);
        });

        verify(memberService).findByIdOrThrow(member.getId());
        verify(alarmService).sendToClient(
                eq(member.getId()), eq(AuthenticateAlarmTemplate.SAVE_VERIFICATION.name()), any(Alarm.class));
        verify(pushNotificationService).send(
                member, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.SAVE_VERIFICATION);
    }

    @Test
    @DisplayName("인증 반려 트랜잭션이 커밋된 뒤에만 반려 사유를 담은 알림을 전송한다")
    void sendsRejectionNotificationsOnlyAfterTransactionCommits() {
        AuthenticationApprove approve = pendingApproval();
        Member member = Member.builder().id(10L).build();
        String rejectReason = "소속을 확인할 수 없습니다.";
        given(authenticationApproveRepository.findById(approve.getId())).willReturn(Optional.of(approve));
        given(memberService.findByIdOrThrow(member.getId())).willReturn(member);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            authenticationService.deleteVerifications(approve.getId(), rejectReason);

            assertThat(approve.getStatus()).isEqualTo(ApproveType.REJECT);
            assertThat(approve.getRejectReason()).isEqualTo(rejectReason);
            verifyNoInteractions(memberService, alarmService, pushNotificationService);
        });

        verify(memberService).findByIdOrThrow(member.getId());
        verify(alarmService).sendToClient(
                eq(member.getId()), eq(AuthenticateAlarmTemplate.DELETE_VERIFICATION.name()), any(Alarm.class));
        verify(pushNotificationService).send(
                member, AlarmSettingType.NOTIFICATION,
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatDeepLink());
    }

    @Test
    @DisplayName("인증 승인 트랜잭션이 롤백되면 알림을 전송하지 않는다")
    void doesNotSendApprovalNotificationsWhenTransactionRollsBack() {
        AuthenticationApprove approve = pendingApproval();
        given(authenticationApproveRepository.findById(approve.getId())).willReturn(Optional.of(approve));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            authenticationService.saveVerifications(approve.getId());
            status.setRollbackOnly();
        });

        verify(authenticationApproveRepository).findById(approve.getId());
        verifyNoInteractions(memberService, alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("인증 반려 트랜잭션이 롤백되면 알림을 전송하지 않는다")
    void doesNotSendRejectionNotificationsWhenTransactionRollsBack() {
        AuthenticationApprove approve = pendingApproval();
        given(authenticationApproveRepository.findById(approve.getId())).willReturn(Optional.of(approve));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            authenticationService.deleteVerifications(approve.getId(), "반려 사유");
            status.setRollbackOnly();
        });

        verify(authenticationApproveRepository).findById(approve.getId());
        verifyNoInteractions(memberService, alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("트랜잭션 없이 발행한 승인·반려 이벤트는 알림을 전송하지 않는다")
    void doesNotSendNotificationsWhenEventsArePublishedOutsideTransaction() {
        eventPublisher.publishEvent(new VerificationSaveEvent(10L));
        eventPublisher.publishEvent(new VerificationDeleteEvent(10L, "반려 사유"));

        verifyNoInteractions(memberService, alarmService, pushNotificationService);
    }

    private AuthenticationApprove pendingApproval() {
        return AuthenticationApprove.builder()
                .id(100L)
                .authentication(Authentication.builder().memberId(10L).build())
                .status(ApproveType.PENDING)
                .build();
    }

    @Configuration
    @EnableTransactionManagement
    @Import({AuthenticationServiceImpl.class, AuthenticationEventListener.class})
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:verification-events;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        AuthenticationRepository authenticationRepository() {
            return mock(AuthenticationRepository.class);
        }

        @Bean
        AuthenticationApproveRepository authenticationApproveRepository() {
            return mock(AuthenticationApproveRepository.class);
        }

        @Bean
        MailSendService mailSendService() {
            return mock(MailSendService.class);
        }

        @Bean
        ResourceLoader resourceLoader() {
            return mock(ResourceLoader.class);
        }

        @Bean
        MemberServiceImpl memberService() {
            return mock(MemberServiceImpl.class);
        }

        @Bean
        AlarmServiceImpl alarmService() {
            return mock(AlarmServiceImpl.class);
        }

        @Bean
        PushNotificationServiceImpl pushNotificationService() {
            return mock(PushNotificationServiceImpl.class);
        }
    }
}
