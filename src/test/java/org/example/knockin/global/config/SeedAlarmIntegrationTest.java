package org.example.knockin.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.knockin.support.IsolatedExternalServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@IsolatedExternalServices
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed-alarm-regression;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.batch.job.enabled=false",
        "seed.load-test.records-per-entity=3",
        "logging.level.org.hibernate.SQL=WARN"
})
@ActiveProfiles("test")
class SeedAlarmIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void loadTestAlarmsReferencePersistedRequestsAndCalendars() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM roommate_matching_required_alarm a
                JOIN alarm base ON base.id = a.id
                JOIN roommate_matching_required r ON r.id = a.roommate_matching_required_id
                WHERE base.title = '부하 테스트 매칭 알림' AND base.member_id = r.requester_id
                """, Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM roommate_calendar_alarm a
                JOIN alarm base ON base.id = a.id
                JOIN roommate_calendar c ON c.id = a.roommate_calendar_id
                WHERE base.title = '부하 테스트 일정 알림' AND base.member_id = c.member_id
                """, Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM chatting_required_alarm a
                JOIN alarm base ON base.id = a.id
                JOIN chatting_required r ON r.id = a.chatting_required_id
                WHERE base.title = '부하 테스트 채팅 알림'
                """, Long.class)).isEqualTo(3L);
    }
}
