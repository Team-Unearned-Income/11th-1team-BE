package org.example.knockin.inquiry.service.impl;

import java.util.Optional;
import javax.sql.DataSource;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.InquiryAlarmTemplate;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.inquiry.repository.InquiryCategoryRepository;
import org.example.knockin.inquiry.repository.InquiryCommentRepository;
import org.example.knockin.inquiry.repository.InquiryRepository;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.example.knockin.inquiry.service.InquiryService;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

@SpringJUnitConfig(InquiryReplyEventIntegrationTest.TestConfig.class)
@DisplayName("문의 답변 이벤트의 트랜잭션 처리 테스트")
class InquiryReplyEventIntegrationTest {

    @Autowired private InquiryService inquiryService;
    @Autowired private InquiryCommentRepository inquiryCommentRepository;
    @Autowired private MemberServiceImpl memberService;
    @Autowired private AlarmServiceImpl alarmService;
    @Autowired private PushNotificationServiceImpl pushNotificationService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetMocks() {
        reset(inquiryCommentRepository, memberService, alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("답변 저장 트랜잭션이 커밋된 뒤에만 작성자에게 알림을 전송한다")
    void sendsNotificationsOnlyAfterReplyTransactionCommits() {
        InquiryComment comment = reply();
        Member member = Member.builder().id(10L).build();
        given(inquiryCommentRepository.save(comment)).willReturn(comment);
        given(memberService.findById(member.getId())).willReturn(Optional.of(member));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(inquiryService.saveInquiryReply(comment)).isSameAs(comment);

            verify(inquiryCommentRepository).save(comment);
            verifyNoInteractions(memberService, alarmService, pushNotificationService);
        });

        verify(memberService).findById(member.getId());
        verify(alarmService).sendToClient(
                eq(member.getId()), eq(InquiryAlarmTemplate.INQUIRIE_REPLY.name()), any(Alarm.class));
        verify(pushNotificationService).send(
                member, AlarmSettingType.NOTIFICATION, InquiryAlarmTemplate.INQUIRIE_REPLY);
    }

    @Test
    @DisplayName("답변 저장 트랜잭션이 롤백되면 알림을 전송하지 않는다")
    void doesNotSendNotificationsWhenReplyTransactionRollsBack() {
        InquiryComment comment = reply();
        given(inquiryCommentRepository.save(comment)).willReturn(comment);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            inquiryService.saveInquiryReply(comment);
            status.setRollbackOnly();
        });

        verify(inquiryCommentRepository).save(comment);
        verifyNoInteractions(memberService, alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("트랜잭션 없이 발행한 답변 이벤트는 알림을 전송하지 않는다")
    void doesNotSendNotificationsWhenEventIsPublishedOutsideTransaction() {
        eventPublisher.publishEvent(new InquiryReplyEvent(10L, 100L));

        verifyNoInteractions(memberService, alarmService, pushNotificationService);
    }

    private InquiryComment reply() {
        return InquiryComment.builder()
                .inquiry(Inquiry.builder().id(100L).memberId(10L).build())
                .memberId(20L)
                .contents("답변 내용")
                .build();
    }

    @Configuration
    @EnableTransactionManagement
    @Import({InquiryServiceImpl.class, InquiryEventListener.class})
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:inquiry-reply-events;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        InquiryCategoryRepository inquiryCategoryRepository() {
            return mock(InquiryCategoryRepository.class);
        }

        @Bean
        InquiryRepository inquiryRepository() {
            return mock(InquiryRepository.class);
        }

        @Bean
        InquiryCommentRepository inquiryCommentRepository() {
            return mock(InquiryCommentRepository.class);
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
