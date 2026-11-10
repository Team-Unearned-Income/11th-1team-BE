package org.example.knockin.alarm.repository.impl;

import com.querydsl.jpa.JPQLSubQuery;
import org.example.knockin.alarm.repository.AlarmRepositoryCustom;

import static org.example.knockin.alarm.entity.QAlarm.alarm;
import static org.example.knockin.alarm.entity.QAlarmSetting.alarmSetting;
import static org.example.knockin.alarm.entity.QChattingRequiredAlarm.chattingRequiredAlarm;
import static org.example.knockin.alarm.entity.QNotificationAlarm.notificationAlarm;
import static org.example.knockin.alarm.entity.QRoommateCalendarAlarm.roommateCalendarAlarm;
import static org.example.knockin.alarm.entity.QRoommateMatchingRequiredAlarm.roommateMatchingRequiredAlarm;
import static org.example.knockin.chat.entity.QChattingRequired.chattingRequired;
import static org.example.knockin.mate.entity.QMyRoommate.myRoommate;
import static org.example.knockin.mate.entity.QRoommateCalendar.roommateCalendar;
import static org.example.knockin.mate.entity.QRoommateMatchingRequired.roommateMatchingRequired;

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
    public void deleteForMembers(List<Long> memberIds) {
        if (memberIds.isEmpty()) return;

        JPQLSubQuery<Long> chatRequestIds = JPAExpressions.select(chattingRequired.id).from(chattingRequired)
                .where(chattingRequired.requester.id.in(memberIds).or(chattingRequired.requestee.id.in(memberIds)));

        JPQLSubQuery<Long> matchingRequestIds = JPAExpressions.select(roommateMatchingRequired.id).from(roommateMatchingRequired)
                .where(roommateMatchingRequired.requester.id.in(memberIds).or(roommateMatchingRequired.requestee.id.in(memberIds)).or(roommateMatchingRequired.chattingRoom.chattingRequired.id.in(chatRequestIds)));

        JPQLSubQuery<Long> calendarIds = JPAExpressions.select(roommateCalendar.id).from(roommateCalendar)
                .join(roommateCalendar.myRoommate, myRoommate).where(myRoommate.roommateMatchingRequired.id.in(matchingRequestIds).or(roommateCalendar.member.id.in(memberIds)));

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
