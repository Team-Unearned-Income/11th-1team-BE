package org.example.knockin.alarm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = AlarmRepositoryPersistenceTest.JpaConfiguration.class)
class AlarmRepositoryPersistenceTest {

    @Autowired
    private AlarmSettingRepository settings;

    @Autowired
    private AlarmRepository alarms;

    @Test
    void findsSettingsOnlyForTheRequestedMemberId() {
        AlarmSetting own = saveSetting(10L, true);
        saveSetting(20L, false);

        assertThat(settings.findByMemberId(10L)).extracting(AlarmSetting::getId)
                .containsExactly(own.getId());
        assertThat(settings.findByIdAndMemberId(own.getId(), 10L)).isSameAs(own);
        assertThat(settings.findByIdAndMemberId(own.getId(), 20L)).isNull();
    }

    @Test
    void checksEnabledSettingByMemberIdAndType() {
        saveSetting(10L, true);
        saveSetting(20L, false);

        assertThat(settings.existsByMemberIdAndAlarmSettingTypeAndIsEnabledTrue(
                10L, AlarmSettingType.NOTIFICATION)).isTrue();
        assertThat(settings.existsByMemberIdAndAlarmSettingTypeAndIsEnabledTrue(
                20L, AlarmSettingType.NOTIFICATION)).isFalse();
        assertThat(settings.existsByMemberIdAndAlarmSettingTypeAndIsEnabledTrue(
                30L, AlarmSettingType.NOTIFICATION)).isFalse();
    }

    @Test
    void findsAlarmsByMemberIdAndReadStatusWithoutExposingAnotherMembersAlarm() {
        Alarm unread = saveAlarm(10L, false);
        Alarm read = saveAlarm(10L, true);
        saveAlarm(20L, false);

        assertThat(alarms.findByMemberId(10L, PageRequest.of(0, 10)))
                .extracting(Alarm::getId).containsExactlyInAnyOrder(unread.getId(), read.getId());
        assertThat(alarms.findByMemberIdAndIsRead(10L, false))
                .extracting(Alarm::getId).containsExactly(unread.getId());
        assertThat(alarms.findByIdAndMemberId(unread.getId(), 10L)).contains(unread);
        assertThat(alarms.findByIdAndMemberId(unread.getId(), 20L)).isEmpty();
    }

    private AlarmSetting saveSetting(Long memberId, boolean enabled) {
        return settings.saveAndFlush(AlarmSetting.builder()
                .memberId(memberId).alarmSettingType(AlarmSettingType.NOTIFICATION)
                .isEnabled(enabled).build());
    }

    private Alarm saveAlarm(Long memberId, boolean read) {
        return alarms.saveAndFlush(Alarm.builder()
                .memberId(memberId).title("알림").contents("알림 내용")
                .type(AlarmType.DEFAULT).isRead(read)
                .expiredAt(LocalDateTime.now().plusDays(1)).build());
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan("org.example.knockin")
    @EnableJpaRepositories(basePackageClasses = AlarmRepository.class)
    static class JpaConfiguration {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }
}
