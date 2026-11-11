package org.example.knockin.repository.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.mate.entity.ExcludeRoommateCalendar;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RepeatRoommateCalendar;
import org.example.knockin.mate.entity.RepeatType;
import org.example.knockin.mate.entity.RoommateCalendar;
import org.example.knockin.mate.entity.RoommateCalendarCategory;
import org.example.knockin.mate.entity.RoommateCalendarMember;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.mate.repository.row.DailyCalendarMemberRow;
import org.example.knockin.mate.repository.row.DailyCalendarRow;
import org.example.knockin.mate.repository.row.MonthlyCalendarRow;
import org.example.knockin.mate.repository.row.RepeatCalendarExcludeRow;
import org.example.knockin.mate.repository.ExcludeRoommateCalendarRepository;
import org.example.knockin.mate.repository.RoommateCalendarMemberRepository;
import org.example.knockin.mate.repository.RoommateCalendarRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("룸메이트 캘린더 리포지토리")
class RoommateCalendarRepositoryTest {

    @Autowired
    private RoommateCalendarRepository roommateCalendarRepository;

    @Autowired
    private RoommateCalendarMemberRepository roommateCalendarMemberRepository;

    @Autowired
    private ExcludeRoommateCalendarRepository excludeRoommateCalendarRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("특정일 일정 후보 조회 시 일반 일정과 활성 반복 마스터와 담당자와 제외일을 벌크로 조회한다")
    void findDailyCalendarListReturnsBasicRepeatMembersAndExcludes() {
        // Given
        Member requester = persistMember("calendar-requester", "요청자");
        Member requestee = persistMember("calendar-requestee", "요청받은 사람");
        MyRoommate myRoommate = persistMyRoommate(requester, requestee);
        RoommateCalendarCategory category = persistCategory("청소");
        RoommateCalendar basicCalendar = persistCalendar(
                myRoommate,
                requester,
                category,
                "장보기",
                "저녁 재료",
                LocalDateTime.of(2026, 7, 12, 9, 0),
                LocalDateTime.of(2026, 7, 12, 10, 0)
        );
        RoommateCalendar repeatMaster = persistCalendar(
                myRoommate,
                requester,
                category,
                "청소",
                "거실 청소",
                LocalDateTime.of(2026, 7, 5, 14, 0),
                LocalDateTime.of(2026, 7, 5, 16, 0)
        );
        RepeatRoommateCalendar repeatCalendar = persistRepeatCalendar(
                repeatMaster,
                LocalDateTime.of(2026, 7, 26, 16, 0),
                RepeatType.WEEKLY
        );
        persistCalendarMember(basicCalendar, requester);
        persistCalendarMember(repeatMaster, requester);
        persistCalendarMember(repeatMaster, requestee);
        persistExclude(repeatCalendar, LocalDateTime.of(2026, 7, 12, 14, 0));
        entityManager.flush();
        entityManager.clear();

        LocalDateTime from = LocalDateTime.of(2026, 7, 12, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 13, 0, 0);

        // When
        List<DailyCalendarRow> calendars = roommateCalendarRepository.findDailyCalendarList(myRoommate.getId(), from, to);
        List<DailyCalendarMemberRow> members = roommateCalendarMemberRepository.findDailyCalendarMembers(
                List.of(basicCalendar.getId(), repeatMaster.getId())
        );
        List<RepeatCalendarExcludeRow> excludes = excludeRoommateCalendarRepository.findRepeatCalendarExcludes(
                List.of(repeatCalendar.getId())
        );
        List<MonthlyCalendarRow> monthlyCalendars = roommateCalendarRepository.findMonthlyCalendarList(myRoommate.getId(), from, to);

        // Then
        assertThat(calendars)
                .extracting(DailyCalendarRow::calendarId, DailyCalendarRow::repeatCalendarId, DailyCalendarRow::repeatEndDate, DailyCalendarRow::repeatType)
                .containsExactly(
                        tuple(repeatMaster.getId(), repeatCalendar.getId(), LocalDateTime.of(2026, 7, 26, 16, 0), RepeatType.WEEKLY),
                        tuple(basicCalendar.getId(), null, null, null)
                );
        assertThat(members)
                .extracting(DailyCalendarMemberRow::calendarId, DailyCalendarMemberRow::memberId, DailyCalendarMemberRow::name)
                .containsExactly(
                        tuple(basicCalendar.getId(), requester.getId(), "요청자"),
                        tuple(repeatMaster.getId(), requester.getId(), "요청자"),
                        tuple(repeatMaster.getId(), requestee.getId(), "요청받은 사람")
                );
        assertThat(excludes)
                .extracting(RepeatCalendarExcludeRow::repeatCalendarId, RepeatCalendarExcludeRow::excludeAt)
                .containsExactly(tuple(repeatCalendar.getId(), LocalDateTime.of(2026, 7, 12, 14, 0)));
        assertThat(monthlyCalendars)
                .extracting(MonthlyCalendarRow::calendarId, MonthlyCalendarRow::startDate, MonthlyCalendarRow::endDate,
                        MonthlyCalendarRow::repeatCalendarId, MonthlyCalendarRow::repeatEndDate, MonthlyCalendarRow::repeatType)
                .containsExactly(
                        tuple(repeatMaster.getId(), LocalDateTime.of(2026, 7, 5, 14, 0), LocalDateTime.of(2026, 7, 5, 16, 0),
                                repeatCalendar.getId(), LocalDateTime.of(2026, 7, 26, 16, 0), RepeatType.WEEKLY),
                        tuple(basicCalendar.getId(), LocalDateTime.of(2026, 7, 12, 9, 0), LocalDateTime.of(2026, 7, 12, 10, 0),
                                null, null, null)
                );
    }

    private Member persistMember(String providerId, String name) {
        Member member = Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId(providerId)
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        entityManager.persist(member);
        entityManager.persist(BasicInformation.builder()
                .member(member)
                .name(name)
                .birth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .email(providerId + "@test.com")
                .build());
        return member;
    }

    private MyRoommate persistMyRoommate(Member requester, Member requestee) {
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requesterId(requester.getId())
                .requesteeId(requestee.getId())
                .status(ChattingRequiredStatus.ACCEPTED)
                .build();
        entityManager.persist(chattingRequired);

        ChattingRoom chattingRoom = ChattingRoom.builder()
                .chattingRequired(chattingRequired)
                .build();
        entityManager.persist(chattingRoom);

        RoommateMatchingRequired matchingRequired = RoommateMatchingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .chattingRoomId(chattingRoom.getId())
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();
        entityManager.persist(matchingRequired);

        MyRoommate myRoommate = MyRoommate.builder()
                .roommateMatchingRequired(matchingRequired)
                .isDeleted(false)
                .build();
        entityManager.persist(myRoommate);
        return myRoommate;
    }

    private RoommateCalendarCategory persistCategory(String name) {
        RoommateCalendarCategory category = RoommateCalendarCategory.builder()
                .name(name)
                .isDeleted(false)
                .build();
        entityManager.persist(category);
        return category;
    }

    private RoommateCalendar persistCalendar(
            MyRoommate myRoommate,
            Member owner,
            RoommateCalendarCategory category,
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        RoommateCalendar calendar = RoommateCalendar.builder()
                .myRoommate(myRoommate)
                .member(owner)
                .roommateCalendarCategory(category)
                .title(title)
                .contents(contents)
                .startDate(startDate)
                .endDate(endDate)
                .isDeleted(false)
                .build();
        entityManager.persist(calendar);
        return calendar;
    }

    private RepeatRoommateCalendar persistRepeatCalendar(RoommateCalendar calendar, LocalDateTime endDate, RepeatType repeatType) {
        RepeatRoommateCalendar repeatCalendar = RepeatRoommateCalendar.builder()
                .roommateCalendar(calendar)
                .endDate(endDate)
                .repeatType(repeatType)
                .build();
        entityManager.persist(repeatCalendar);
        return repeatCalendar;
    }

    private void persistCalendarMember(RoommateCalendar calendar, Member member) {
        entityManager.persist(RoommateCalendarMember.of(calendar, member));
    }

    private void persistExclude(RepeatRoommateCalendar repeatCalendar, LocalDateTime excludeAt) {
        entityManager.persist(ExcludeRoommateCalendar.builder()
                .repeatRoommateCalendar(repeatCalendar)
                .excludeAt(excludeAt)
                .build());
    }
}
