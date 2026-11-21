package org.example.knockin.alarm.repository.impl;

import org.example.knockin.alarm.repository.AlarmRepositoryCustom;

import static org.example.knockin.alarm.entity.QAlarm.alarm;
import static org.example.knockin.alarm.entity.QAlarmSetting.alarmSetting;
import static org.example.knockin.alarm.entity.QChattingRequiredAlarm.chattingRequiredAlarm;
import static org.example.knockin.alarm.entity.QNotificationAlarm.notificationAlarm;
import static org.example.knockin.alarm.entity.QRoommateCalendarAlarm.roommateCalendarAlarm;
import static org.example.knockin.alarm.entity.QRoommateMatchingRequiredAlarm.roommateMatchingRequiredAlarm;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlarmRepositoryImpl implements AlarmRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public void deleteForMembers(List<Long> memberIds, List<Long> chatRequestIds, List<Long> matchingRequestIds, List<Long> calendarIds) {
        if (memberIds.isEmpty()) return;

        List<Long> alarmIds = queryFactory.select(alarm.id).from(alarm)
                .where(alarm.memberId.in(memberIds).or(alarm.id.in(JPAExpressions.select(chattingRequiredAlarm.id).from(chattingRequiredAlarm)
                .where(chattingRequiredAlarm.chattingRequiredId.in(chatRequestIds)))).or(alarm.id.in(JPAExpressions.select(roommateMatchingRequiredAlarm.id).from(roommateMatchingRequiredAlarm)
                .where(roommateMatchingRequiredAlarm.roommateMatchingRequiredId.in(matchingRequestIds)))).or(alarm.id.in(JPAExpressions.select(roommateCalendarAlarm.id).from(roommateCalendarAlarm)
                .where(roommateCalendarAlarm.roommateCalendarId.in(calendarIds))))).fetch();

        if (!alarmIds.isEmpty()) {
            queryFactory.delete(notificationAlarm).where(notificationAlarm.alarm.id.in(alarmIds)).execute();
            queryFactory.delete(alarm).where(alarm.id.in(alarmIds)).execute();
        }

        queryFactory.delete(alarmSetting).where(alarmSetting.memberId.in(memberIds)).execute();
    }
}
