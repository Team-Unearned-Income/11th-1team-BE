package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.example.knockin.mate.dto.CalendarDto;
import org.example.knockin.mate.dto.CalendarEditDto;
import org.example.knockin.mate.dto.MyRoommateDailyCalendarListDto;
import org.example.knockin.mate.dto.MyRoommateMonthlyCalendarListDto;
import org.example.knockin.mate.dto.RepeatCalendarDto;
import org.example.knockin.mate.dto.RepeatCalendarModifyDto;
import org.example.knockin.mate.dto.RepeatCalendarModifyType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.mate.entity.ExcludeRoommateCalendar;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RepeatRoommateCalendar;
import org.example.knockin.mate.entity.RepeatType;
import org.example.knockin.mate.entity.RoommateCalendar;
import org.example.knockin.mate.entity.RoommateCalendarCategory;
import org.example.knockin.mate.entity.RoommateCalendarMember;
import org.example.knockin.mate.entity.RoommateCalendarMemberId;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MyRoommateErrorCode;
import org.example.knockin.mate.service.impl.CalendarServiceImpl;
import org.example.knockin.mate.service.impl.ExcludeRoommateCalendarServiceImpl;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.row.MemberWithNameRow;
import org.example.knockin.mate.repository.ExcludeRoommateCalendarRepository;
import org.example.knockin.mate.repository.RepeatRoommateCalendarRepository;
import org.example.knockin.mate.repository.RoommateCalendarCategoryRepository;
import org.example.knockin.mate.repository.RoommateCalendarMemberRepository;
import org.example.knockin.mate.repository.RoommateCalendarRepository;
import org.example.knockin.mate.repository.row.DailyCalendarMemberRow;
import org.example.knockin.mate.repository.row.DailyCalendarRow;
import org.example.knockin.mate.repository.row.MonthlyCalendarRow;
import org.example.knockin.mate.repository.row.RepeatCalendarExcludeRow;
import org.example.knockin.mate.service.impl.RepeatRoommateCalendarServiceImpl;
import org.example.knockin.mate.service.impl.RoommateCalendarCategoryServiceImpl;
import org.example.knockin.mate.service.impl.RoommateCalendarMemberServiceImpl;
import org.example.knockin.mate.service.impl.RoommateCalendarServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 캘린더 서비스")
class CalendarServiceImplTest {

    @Mock
    private RoommateCalendarCategoryRepository roommateCalendarCategoryRepository;

    @Mock
    private RoommateCalendarRepository roommateCalendarRepository;

    @Mock
    private RoommateCalendarMemberRepository roommateCalendarMemberRepository;

    @Mock
    private RepeatRoommateCalendarRepository repeatRoommateCalendarRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ExcludeRoommateCalendarRepository excludeRoommateCalendarRepository;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    @BeforeEach
    void setUp() {
        MemberServiceImpl memberService = new MemberServiceImpl(
                memberRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null, null, null);
        calendarService = new CalendarServiceImpl(
                new RoommateCalendarCategoryServiceImpl(roommateCalendarCategoryRepository),
                new RoommateCalendarServiceImpl(roommateCalendarRepository),
                new RoommateCalendarMemberServiceImpl(roommateCalendarMemberRepository),
                new RepeatRoommateCalendarServiceImpl(repeatRoommateCalendarRepository),
                memberService,
                new ExcludeRoommateCalendarServiceImpl(excludeRoommateCalendarRepository)
        );
    }

    @Test
    @DisplayName("내 룸메이트가 있으면 일반 일정의 카테고리와 캘린더와 담당자를 저장하고 수정 시간을 반환한다")
    void saveCalendarSavesCategoryBasicCalendarAndMembersWhenMyRoommateExists() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        CalendarDto.Request request = calendarRequest(
                "장보기",
                "저녁 재료 사오기",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                LocalDateTime.of(2026, 7, 4, 11, 0),
                "생활",
                List.of(requesterId, requesteeId)
        );
        given(roommateCalendarCategoryRepository.save(any(RoommateCalendarCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .willAnswer(invocation -> savedCalendar(100L, invocation.getArgument(0)));

        // When
        CalendarDto.Response response = calendarService.saveBasic(requesterId, myRoommate, request);

        // Then
        ArgumentCaptor<RoommateCalendarCategory> categoryCaptor = ArgumentCaptor.forClass(RoommateCalendarCategory.class);
        verify(roommateCalendarCategoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("생활");

        ArgumentCaptor<RoommateCalendar> calendarCaptor = ArgumentCaptor.forClass(RoommateCalendar.class);
        verify(roommateCalendarRepository).save(calendarCaptor.capture());
        RoommateCalendar savedCalendar = calendarCaptor.getValue();
        assertThat(savedCalendar.getMyRoommate()).isSameAs(myRoommate);
        assertThat(savedCalendar.getMember()).isSameAs(myRoommate.getRoommateMatchingRequired().getRequester());
        assertThat(savedCalendar.getRoommateCalendarCategory().getName()).isEqualTo("생활");
        assertThat(savedCalendar.getTitle()).isEqualTo("장보기");
        assertThat(savedCalendar.getContents()).isEqualTo("저녁 재료 사오기");
        assertThat(savedCalendar.getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 4, 10, 0));
        assertThat(savedCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 4, 11, 0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoommateCalendarMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(roommateCalendarMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getMember().getId())
                .containsExactly(requesterId, requesteeId);
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getRoommateCalendar().getId())
                .containsExactly(100L, 100L);
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getId().getMemberId())
                .containsExactly(requesterId, requesteeId);
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getId().getRoommateCalendarId())
                .containsExactly(100L, 100L);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(repeatRoommateCalendarRepository, never()).save(any(RepeatRoommateCalendar.class));
    }

    @Test
    @DisplayName("일반 일정 저장 시 룸메이트 구성원이 아닌 담당자가 포함되면 접근 거부 예외를 던지고 담당자를 저장하지 않는다")
    void saveBasicThrowsWhenMemberIdsContainNonRoommateMember() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long nonRoommateMemberId = 999L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);

        // When & Then
        assertThatThrownBy(() -> calendarService.saveBasic(requesterId, myRoommate, calendarRequest(
                "장보기",
                "저녁 재료 사오기",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                LocalDateTime.of(2026, 7, 4, 11, 0),
                "생활",
                List.of(requesterId, nonRoommateMemberId)
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_ACCESS_DENIED));
        verify(roommateCalendarMemberRepository, never()).saveAll(any());
        verify(repeatRoommateCalendarRepository, never()).save(any(RepeatRoommateCalendar.class));
    }

    @Test
    @DisplayName("내 룸메이트가 있으면 반복 일정의 기본 캘린더와 반복 정보를 함께 저장하고 수정 시간을 반환한다")
    void saveRepeatCalendarSavesBasicCalendarMembersAndRepeatInfoWhenMyRoommateExists() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        RepeatCalendarDto.Request request = repeatCalendarRequest(
                "청소",
                "거실 청소하기",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0),
                "청소",
                LocalDateTime.of(2026, 8, 1, 10, 0),
                RepeatType.WEEKLY,
                List.of(requesterId, requesteeId)
        );
        given(roommateCalendarCategoryRepository.save(any(RoommateCalendarCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .willAnswer(invocation -> savedCalendar(200L, invocation.getArgument(0)));

        // When
        RepeatCalendarDto.Response response = calendarService.saveRepeat(requesterId, myRoommate, request);

        // Then
        ArgumentCaptor<RoommateCalendarCategory> categoryCaptor = ArgumentCaptor.forClass(RoommateCalendarCategory.class);
        verify(roommateCalendarCategoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("청소");

        ArgumentCaptor<RoommateCalendar> calendarCaptor = ArgumentCaptor.forClass(RoommateCalendar.class);
        verify(roommateCalendarRepository).save(calendarCaptor.capture());
        RoommateCalendar savedCalendar = calendarCaptor.getValue();
        assertThat(savedCalendar.getMyRoommate()).isSameAs(myRoommate);
        assertThat(savedCalendar.getMember()).isSameAs(myRoommate.getRoommateMatchingRequired().getRequester());
        assertThat(savedCalendar.getRoommateCalendarCategory().getName()).isEqualTo("청소");
        assertThat(savedCalendar.getTitle()).isEqualTo("청소");
        assertThat(savedCalendar.getContents()).isEqualTo("거실 청소하기");
        assertThat(savedCalendar.getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 4, 9, 0));
        assertThat(savedCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 4, 10, 0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoommateCalendarMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(roommateCalendarMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getMember().getId())
                .containsExactly(requesterId, requesteeId);
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getRoommateCalendar().getId())
                .containsExactly(200L, 200L);

        ArgumentCaptor<RepeatRoommateCalendar> repeatCaptor = ArgumentCaptor.forClass(RepeatRoommateCalendar.class);
        verify(repeatRoommateCalendarRepository).save(repeatCaptor.capture());
        assertThat(repeatCaptor.getValue().getRoommateCalendar().getId()).isEqualTo(200L);
        assertThat(repeatCaptor.getValue().getEndDate()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 0));
        assertThat(repeatCaptor.getValue().getRepeatType()).isEqualTo(RepeatType.WEEKLY);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("반복 일정 저장 시 룸메이트 구성원이 아닌 담당자가 포함되면 접근 거부 예외를 던지고 담당자와 반복 정보를 저장하지 않는다")
    void saveRepeatThrowsWhenMemberIdsContainNonRoommateMember() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long nonRoommateMemberId = 999L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);

        // When & Then
        assertThatThrownBy(() -> calendarService.saveRepeat(requesterId, myRoommate, repeatCalendarRequest(
                "청소",
                "거실 청소하기",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0),
                "청소",
                LocalDateTime.of(2026, 8, 1, 10, 0),
                RepeatType.WEEKLY,
                List.of(requesterId, nonRoommateMemberId)
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_ACCESS_DENIED));
        verify(roommateCalendarMemberRepository, never()).saveAll(any());
        verify(repeatRoommateCalendarRepository, never()).save(any(RepeatRoommateCalendar.class));
    }

    @Test
    @DisplayName("내 룸메이트가 있으면 캘린더 편집 폼에 반복 타입과 담당자 목록과 카테고리명을 반환한다")
    void getRoommateEditFormReturnsRepeatTypesMembersAndCategoryNamesWhenMyExists() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        given(memberRepository.findAllWithNameRowById(List.of(requesterId, requesteeId)))
                .willReturn(List.of(
                        new MemberWithNameRow(requesterId, "요청자"),
                        new MemberWithNameRow(requesteeId, "요청받은 사람")
                ));

        // When
        CalendarEditDto.Response response = calendarService.getEditForm(requesteeId, myRoommate);

        // Then
        assertThat(response.getRepeatType()).containsExactly(RepeatType.WEEKLY, RepeatType.BI_WEEKLY, RepeatType.MONTHLY);
        assertThat(response.getMembers())
                .extracting(CalendarEditDto.MemberInfo::getMemberId, CalendarEditDto.MemberInfo::getName, CalendarEditDto.MemberInfo::getIsMe)
                .containsExactly(
                        tuple(requesterId, "요청자", false),
                        tuple(requesteeId, "요청받은 사람", true)
                );
        assertThat(response.getCategoryNames()).containsExactly("청소", "공과금", "기타");
        verify(memberRepository).findAllWithNameRowById(List.of(requesterId, requesteeId));
        verifyNoInteractions(roommateCalendarCategoryRepository, roommateCalendarRepository,
                roommateCalendarMemberRepository, repeatRoommateCalendarRepository);
    }

    @Test
    @DisplayName("특정일 일정 조회 시 일반 일정과 반복 일정 발생분을 담당자와 함께 반환한다")
    void findDailyListReturnsBasicAndRepeatOccurrenceWithMembers() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        LocalDateTime from = LocalDateTime.of(2026, 7, 12, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 13, 0, 0);
        DailyCalendarRow basicCalendar = dailyCalendarRow(
                100L,
                "장보기",
                "저녁 재료 사오기",
                LocalDateTime.of(2026, 7, 12, 9, 0),
                LocalDateTime.of(2026, 7, 12, 10, 0),
                "생활",
                null,
                null,
                null
        );
        DailyCalendarRow repeatCalendar = dailyCalendarRow(
                200L,
                "청소",
                "거실 청소",
                LocalDateTime.of(2026, 7, 5, 14, 0),
                LocalDateTime.of(2026, 7, 5, 16, 0),
                "청소",
                300L,
                LocalDateTime.of(2026, 7, 26, 16, 0),
                RepeatType.WEEKLY
        );
        given(roommateCalendarRepository.findDailyCalendarList(myRoommate.getId(), from, to))
                .willReturn(List.of(basicCalendar, repeatCalendar));
        given(roommateCalendarMemberRepository.findDailyCalendarMembers(List.of(100L, 200L)))
                .willReturn(List.of(
                        new DailyCalendarMemberRow(100L, requesterId, "요청자"),
                        new DailyCalendarMemberRow(200L, requesterId, "요청자"),
                        new DailyCalendarMemberRow(200L, requesteeId, "요청받은 사람")
                ));
        given(excludeRoommateCalendarRepository.findRepeatCalendarExcludes(List.of(300L))).willReturn(List.of());

        // When
        MyRoommateDailyCalendarListDto.Response response = calendarService.findDailyList(myRoommate, 2026, 7, 12);

        // Then
        assertThat(response.getTargetDay()).isEqualTo(LocalDateTime.of(2026, 7, 12, 0, 0).toLocalDate());
        assertThat(response.getCalendars()).hasSize(2);
        assertThat(response.getCalendars())
                .extracting(item -> item.getCalendarBasicInfo().getCalendarId(),
                        item -> item.getCalendarBasicInfo().getStartDate(),
                        item -> item.getCalendarBasicInfo().getEndDate(),
                        item -> item.getCalendarBasicInfo().getRepeatType())
                .containsExactly(
                        tuple(100L, LocalDateTime.of(2026, 7, 12, 9, 0), LocalDateTime.of(2026, 7, 12, 10, 0), null),
                        tuple(200L, LocalDateTime.of(2026, 7, 12, 14, 0), LocalDateTime.of(2026, 7, 12, 16, 0), RepeatType.WEEKLY)
                );
        assertThat(response.getCalendars().get(1).getCalendarMembers())
                .extracting(MyRoommateDailyCalendarListDto.CalendarMember::getMemberId, MyRoommateDailyCalendarListDto.CalendarMember::getName)
                .containsExactly(
                        tuple(requesterId, "요청자"),
                        tuple(requesteeId, "요청받은 사람")
                );
        verify(roommateCalendarMemberRepository).findDailyCalendarMembers(List.of(100L, 200L));
        verify(excludeRoommateCalendarRepository).findRepeatCalendarExcludes(List.of(300L));
    }

    @Test
    @DisplayName("특정일 일정 조회 시 제외된 반복 발생분은 반환하지 않고 대체 단일 일정만 반환한다")
    void findDailyListSkipsExcludedRepeatOccurrence() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        LocalDateTime from = LocalDateTime.of(2026, 7, 12, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 13, 0, 0);
        DailyCalendarRow replacementCalendar = dailyCalendarRow(
                201L,
                "수정된 청소",
                "욕실 청소",
                LocalDateTime.of(2026, 7, 12, 12, 0),
                LocalDateTime.of(2026, 7, 12, 13, 0),
                "청소",
                null,
                null,
                null
        );
        DailyCalendarRow repeatCalendar = dailyCalendarRow(
                200L,
                "청소",
                "거실 청소",
                LocalDateTime.of(2026, 7, 5, 14, 0),
                LocalDateTime.of(2026, 7, 5, 16, 0),
                "청소",
                300L,
                LocalDateTime.of(2026, 7, 26, 16, 0),
                RepeatType.WEEKLY
        );
        given(roommateCalendarRepository.findDailyCalendarList(myRoommate.getId(), from, to))
                .willReturn(List.of(replacementCalendar, repeatCalendar));
        given(roommateCalendarMemberRepository.findDailyCalendarMembers(List.of(201L, 200L)))
                .willReturn(List.of(
                        new DailyCalendarMemberRow(201L, requesterId, "요청자"),
                        new DailyCalendarMemberRow(200L, requesteeId, "요청받은 사람")
                ));
        given(excludeRoommateCalendarRepository.findRepeatCalendarExcludes(List.of(300L)))
                .willReturn(List.of(new RepeatCalendarExcludeRow(300L, LocalDateTime.of(2026, 7, 12, 14, 0))));

        // When
        MyRoommateDailyCalendarListDto.Response response = calendarService.findDailyList(myRoommate, 2026, 7, 12);

        // Then
        assertThat(response.getTargetDay()).isEqualTo(LocalDateTime.of(2026, 7, 12, 0, 0).toLocalDate());
        assertThat(response.getCalendars()).hasSize(1);
        assertThat(response.getCalendars().getFirst().getCalendarBasicInfo().getCalendarId()).isEqualTo(201L);
        assertThat(response.getCalendars().getFirst().getCalendarBasicInfo().getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 12, 12, 0));
        assertThat(response.getCalendars().getFirst().getCalendarBasicInfo().getRepeatType()).isNull();
    }

    @Test
    @DisplayName("특정일 일정 조회 시 연속일 반복 일정이 조회일에 걸치면 해당 발생분을 반환한다")
    void findDailyListReturnsMultiDayRepeatOccurrenceWhenItOverlapsTargetDay() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        LocalDateTime from = LocalDateTime.of(2026, 7, 13, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 14, 0, 0);
        DailyCalendarRow repeatCalendar = dailyCalendarRow(
                200L,
                "여행",
                "연속 일정",
                LocalDateTime.of(2026, 7, 5, 14, 0),
                LocalDateTime.of(2026, 7, 7, 16, 0),
                "기타",
                300L,
                LocalDateTime.of(2026, 7, 26, 16, 0),
                RepeatType.WEEKLY
        );
        given(roommateCalendarRepository.findDailyCalendarList(myRoommate.getId(), from, to))
                .willReturn(List.of(repeatCalendar));
        given(roommateCalendarMemberRepository.findDailyCalendarMembers(List.of(200L)))
                .willReturn(List.of(new DailyCalendarMemberRow(200L, requesteeId, "요청받은 사람")));
        given(excludeRoommateCalendarRepository.findRepeatCalendarExcludes(List.of(300L))).willReturn(List.of());

        // When
        MyRoommateDailyCalendarListDto.Response response = calendarService.findDailyList(myRoommate, 2026, 7, 13);

        // Then
        assertThat(response.getTargetDay()).isEqualTo(LocalDateTime.of(2026, 7, 13, 0, 0).toLocalDate());
        assertThat(response.getCalendars()).hasSize(1);
        assertThat(response.getCalendars().getFirst().getCalendarBasicInfo().getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 12, 14, 0));
        assertThat(response.getCalendars().getFirst().getCalendarBasicInfo().getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 14, 16, 0));
        assertThat(response.getCalendars().getFirst().getCalendarBasicInfo().getRepeatType()).isEqualTo(RepeatType.WEEKLY);
    }

    @Test
    @DisplayName("월별 일정 조회 시 일반 일정과 반복 일정이 존재하는 날짜를 표시하고 제외된 반복일은 표시하지 않는다")
    void findMyMonthlyListReturnsExistingDaysForBasicAndRepeatCalendars() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(10L, requesterId, requesteeId);
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 1, 0, 0);
        MonthlyCalendarRow multiDayCalendar = monthlyCalendarRow(
                100L,
                LocalDateTime.of(2026, 6, 30, 12, 0),
                LocalDateTime.of(2026, 7, 2, 12, 0),
                null,
                null,
                null
        );
        MonthlyCalendarRow basicCalendar = monthlyCalendarRow(
                101L,
                LocalDateTime.of(2026, 7, 10, 9, 0),
                LocalDateTime.of(2026, 7, 10, 10, 0),
                null,
                null,
                null
        );
        MonthlyCalendarRow repeatCalendar = monthlyCalendarRow(
                200L,
                LocalDateTime.of(2026, 7, 5, 14, 0),
                LocalDateTime.of(2026, 7, 5, 16, 0),
                300L,
                LocalDateTime.of(2026, 7, 26, 16, 0),
                RepeatType.WEEKLY
        );
        given(roommateCalendarRepository.findMonthlyCalendarList(myRoommate.getId(), from, to))
                .willReturn(List.of(multiDayCalendar, basicCalendar, repeatCalendar));
        given(excludeRoommateCalendarRepository.findRepeatCalendarExcludes(List.of(300L)))
                .willReturn(List.of(new RepeatCalendarExcludeRow(300L, LocalDateTime.of(2026, 7, 12, 14, 0))));

        // When
        MyRoommateMonthlyCalendarListDto.Response response = calendarService.findMyMonthlyList(myRoommate, 2026, 7);

        // Then
        assertThat(response.getTargetMonth()).isEqualTo(YearMonth.of(2026, 7));
        assertThat(response.getCalendarDays()).hasSize(31);
        assertThat(response.getCalendarDays())
                .filteredOn(MyRoommateMonthlyCalendarListDto.CalendarDay::getExists)
                .extracting(MyRoommateMonthlyCalendarListDto.CalendarDay::getTargetDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 2),
                        LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 7, 10),
                        LocalDate.of(2026, 7, 19),
                        LocalDate.of(2026, 7, 26)
                );
        assertThat(response.getCalendarDays())
                .filteredOn(calendarDay -> calendarDay.getTargetDate().equals(LocalDate.of(2026, 7, 12)))
                .extracting(MyRoommateMonthlyCalendarListDto.CalendarDay::getExists)
                .containsExactly(false);
        verify(roommateCalendarMemberRepository, never()).findDailyCalendarMembers(any());
    }

    @Test
    @DisplayName("작성자가 일반 일정을 수정하면 일정 내용과 카테고리를 변경하고 새 담당자를 추가한다")
    void modifyCalendarUpdatesCalendarCategoryAndAddsMembersWhenOwnerRequests() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long newMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, newMemberId);
        RoommateCalendar calendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "생활",
                "기존 제목",
                "기존 내용",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        CalendarDto.Request request = calendarRequest(
                "수정 제목",
                "수정 내용",
                LocalDateTime.of(2026, 7, 5, 11, 0),
                LocalDateTime.of(2026, 7, 5, 12, 0),
                "청소",
                List.of(ownerId, newMemberId)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
        given(roommateCalendarMemberRepository.findByRoommateCalendar(calendar))
                .willReturn(List.of(RoommateCalendarMember.of(calendar, member(ownerId))));

        // When
        CalendarDto.Response response = calendarService.modifyCalendar(ownerId, calendarId, request);

        // Then
        assertThat(calendar.getTitle()).isEqualTo("수정 제목");
        assertThat(calendar.getContents()).isEqualTo("수정 내용");
        assertThat(calendar.getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 5, 11, 0));
        assertThat(calendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 5, 12, 0));
        assertThat(calendar.getRoommateCalendarCategory().getName()).isEqualTo("청소");
        assertThat(response.getUpdatedAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoommateCalendarMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(roommateCalendarMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue())
                .extracting(RoommateCalendarMember::getMemberId)
                .containsExactly(newMemberId);
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getRoommateCalendar().getId())
                .containsExactly(calendarId);
        verify(roommateCalendarMemberRepository, never()).deleteById(any(RoommateCalendarMemberId.class));
    }

    @Test
    @DisplayName("작성자가 일반 일정 담당자를 제외하면 기존 담당자 연결을 삭제한다")
    void modifyCalendarDeletesRemovedMembersWhenOwnerRequests() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long removedMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, removedMemberId);
        RoommateCalendar calendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "생활",
                "기존 제목",
                "기존 내용",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        CalendarDto.Request request = calendarRequest(
                "수정 제목",
                "수정 내용",
                LocalDateTime.of(2026, 7, 5, 11, 0),
                LocalDateTime.of(2026, 7, 5, 12, 0),
                "청소",
                List.of(ownerId)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
        given(roommateCalendarMemberRepository.findByRoommateCalendar(calendar))
                .willReturn(List.of(
                        RoommateCalendarMember.of(calendar, member(ownerId)),
                        RoommateCalendarMember.of(calendar, member(removedMemberId))
                ));

        // When
        CalendarDto.Response response = calendarService.modifyCalendar(ownerId, calendarId, request);

        // Then
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(roommateCalendarMemberRepository).saveAll(List.of());

        ArgumentCaptor<RoommateCalendarMemberId> idCaptor = ArgumentCaptor.forClass(RoommateCalendarMemberId.class);
        verify(roommateCalendarMemberRepository).deleteById(idCaptor.capture());
        assertThat(idCaptor.getValue().getRoommateCalendarId()).isEqualTo(calendarId);
        assertThat(idCaptor.getValue().getMemberId()).isEqualTo(removedMemberId);
    }

    @Test
    @DisplayName("일반 일정 수정 시 캘린더가 없으면 캘린더 없음 예외를 던지고 수정하지 않는다")
    void modifyCalendarThrowsWhenCalendarDoesNotExist() {
        // Given
        Long memberId = 1L;
        Long calendarId = 100L;
        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> calendarService.modifyCalendar(memberId, calendarId, calendarRequest(
                "수정 제목",
                "수정 내용",
                LocalDateTime.of(2026, 7, 5, 11, 0),
                LocalDateTime.of(2026, 7, 5, 12, 0),
                "청소",
                List.of(memberId)
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_NOT_FOUND));
        verifyNoInteractions(memberRepository, roommateCalendarMemberRepository);
    }

    @Test
    @DisplayName("일반 일정 수정 시 작성자가 아니면 접근 거부 예외를 던지고 수정하지 않는다")
    void modifyCalendarThrowsWhenMemberIsNotOwner() {
        // Given
        Long ownerId = 1L;
        Long requesterId = 2L;
        Long calendarId = 100L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, requesterId);
        RoommateCalendar calendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "생활",
                "기존 제목",
                "기존 내용",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));

        // When & Then
        assertThatThrownBy(() -> calendarService.modifyCalendar(requesterId, calendarId, calendarRequest(
                "수정 제목",
                "수정 내용",
                LocalDateTime.of(2026, 7, 5, 11, 0),
                LocalDateTime.of(2026, 7, 5, 12, 0),
                "청소",
                List.of(ownerId, requesterId)
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_ACCESS_DENIED));
        assertThat(calendar.getTitle()).isEqualTo("기존 제목");
        assertThat(calendar.getRoommateCalendarCategory().getName()).isEqualTo("생활");
        verifyNoInteractions(memberRepository, roommateCalendarMemberRepository);
    }

    @Test
    @DisplayName("일반 일정 수정 시 룸메이트 구성원이 아닌 담당자가 포함되면 접근 거부 예외를 던지고 담당자를 변경하지 않는다")
    void modifyCalendarThrowsWhenMemberIdsContainNonRoommateMember() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        Long nonRoommateMemberId = 999L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar calendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "생활",
                "기존 제목",
                "기존 내용",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));

        // When & Then
        assertThatThrownBy(() -> calendarService.modifyCalendar(ownerId, calendarId, calendarRequest(
                "수정 제목",
                "수정 내용",
                LocalDateTime.of(2026, 7, 5, 11, 0),
                LocalDateTime.of(2026, 7, 5, 12, 0),
                "청소",
                List.of(ownerId, nonRoommateMemberId)
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_ACCESS_DENIED));
        verify(roommateCalendarMemberRepository, never()).findByRoommateCalendar(any(RoommateCalendar.class));
        verify(roommateCalendarMemberRepository, never()).saveAll(any());
        verify(roommateCalendarMemberRepository, never()).deleteById(any(RoommateCalendarMemberId.class));
    }

    @Test
    @DisplayName("작성자가 일정을 삭제하면 캘린더를 삭제 상태로 변경하고 수정 시간을 반환한다")
    void deleteCalendarSoftDeletesWhenOwnerRequests() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar calendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "생활",
                "삭제할 일정",
                "삭제할 내용",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));

        // When
        CalendarDto.Response response = calendarService.delete(ownerId, calendarId);

        // Then
        assertThat(calendar.getIsDeleted()).isTrue();
        assertThat(response.getUpdatedAt()).isNotNull();
        verifyNoInteractions(memberRepository, roommateCalendarMemberRepository, repeatRoommateCalendarRepository, excludeRoommateCalendarRepository);
    }

    @Test
    @DisplayName("일정 삭제 시 캘린더가 없으면 캘린더 없음 예외를 던지고 삭제하지 않는다")
    void deleteCalendarThrowsWhenDoesNotExist() {
        // Given
        Long memberId = 1L;
        Long calendarId = 100L;
        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> calendarService.delete(memberId, calendarId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_NOT_FOUND));
        verifyNoInteractions(memberRepository, roommateCalendarMemberRepository, repeatRoommateCalendarRepository, excludeRoommateCalendarRepository);
    }

    @Test
    @DisplayName("일정 삭제 시 작성자가 아니면 접근 거부 예외를 던지고 삭제하지 않는다")
    void deleteThrowsWhenMemberIsNotOwner() {
        // Given
        Long ownerId = 1L;
        Long requesterId = 2L;
        Long calendarId = 100L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, requesterId);
        RoommateCalendar calendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "생활",
                "삭제할 일정",
                "삭제할 내용",
                LocalDateTime.of(2026, 7, 4, 9, 0),
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));

        // When & Then
        assertThatThrownBy(() -> calendarService.delete(requesterId, calendarId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_ACCESS_DENIED));
        assertThat(calendar.getIsDeleted()).isFalse();
        verifyNoInteractions(memberRepository, roommateCalendarMemberRepository, repeatRoommateCalendarRepository, excludeRoommateCalendarRepository);
    }

    @Test
    @DisplayName("반복 일정 중 하나만 수정하면 원래 반복 일자를 제외하고 요청 일정으로 단일 일정을 저장한다")
    void modifyRepeatCalendarThisSavesSingleAndExcludesOriginalOccurrence() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar masterCalendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "청소",
                "기존 반복",
                "기존 내용",
                LocalDateTime.of(2026, 7, 5, 16, 0),
                LocalDateTime.of(2026, 7, 5, 18, 0)
        );
        RepeatRoommateCalendar repeatCalendar = repeatRoommateCalendar(
                500L,
                masterCalendar,
                LocalDateTime.of(2026, 8, 31, 18, 0),
                RepeatType.WEEKLY
        );
        RepeatCalendarModifyDto.Request request = repeatModifyRequest(
                RepeatCalendarModifyType.THIS,
                "수정 반복",
                "수정 내용",
                LocalDateTime.of(2026, 7, 11, 12, 0),
                LocalDateTime.of(2026, 7, 13, 20, 0),
                "공과금",
                LocalDateTime.of(2026, 9, 30, 20, 0),
                RepeatType.BI_WEEKLY,
                List.of(ownerId, roommateMemberId),
                LocalDateTime.of(2026, 7, 12, 16, 0),
                LocalDateTime.of(2026, 7, 12, 18, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(masterCalendar));
        given(repeatRoommateCalendarRepository.findOneByRoommateCalendar(masterCalendar)).willReturn(Optional.of(repeatCalendar));
        given(roommateCalendarCategoryRepository.save(any(RoommateCalendarCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .willAnswer(invocation -> savedCalendar(300L, invocation.getArgument(0)));

        // When
        RepeatCalendarModifyDto.Response response = calendarService.modifyRepeat(ownerId, calendarId, myRoommate, request);

        // Then
        ArgumentCaptor<RoommateCalendar> calendarCaptor = ArgumentCaptor.forClass(RoommateCalendar.class);
        verify(roommateCalendarRepository).save(calendarCaptor.capture());
        assertThat(calendarCaptor.getValue().getTitle()).isEqualTo("수정 반복");
        assertThat(calendarCaptor.getValue().getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 11, 12, 0));
        assertThat(calendarCaptor.getValue().getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 13, 20, 0));

        ArgumentCaptor<ExcludeRoommateCalendar> excludeCaptor = ArgumentCaptor.forClass(ExcludeRoommateCalendar.class);
        verify(excludeRoommateCalendarRepository).save(excludeCaptor.capture());
        assertThat(excludeCaptor.getValue().getRepeatRoommateCalendar()).isSameAs(repeatCalendar);
        assertThat(excludeCaptor.getValue().getExcludeAt()).isEqualTo(LocalDateTime.of(2026, 7, 12, 16, 0));
        assertThat(repeatCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 8, 31, 18, 0));
        assertThat(repeatCalendar.getRepeatType()).isEqualTo(RepeatType.WEEKLY);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(repeatRoommateCalendarRepository, never()).save(any(RepeatRoommateCalendar.class));
    }

    @Test
    @DisplayName("반복 일정 중 선택 일자부터 수정하면 기존 반복을 직전까지 끊고 요청 일정으로 새 반복을 저장한다")
    void modifyRepeatCalendarThisAndFollowingSplitsRepeat() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar masterCalendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "청소",
                "기존 반복",
                "기존 내용",
                LocalDateTime.of(2026, 7, 5, 16, 0),
                LocalDateTime.of(2026, 7, 5, 18, 0)
        );
        RepeatRoommateCalendar repeatCalendar = repeatRoommateCalendar(
                500L,
                masterCalendar,
                LocalDateTime.of(2026, 8, 31, 18, 0),
                RepeatType.WEEKLY
        );
        RepeatCalendarModifyDto.Request request = repeatModifyRequest(
                RepeatCalendarModifyType.THIS_AND_FOLLOWING,
                "수정 반복",
                "수정 내용",
                LocalDateTime.of(2026, 7, 11, 12, 0),
                LocalDateTime.of(2026, 7, 13, 20, 0),
                "공과금",
                LocalDateTime.of(2026, 9, 30, 20, 0),
                RepeatType.BI_WEEKLY,
                List.of(ownerId, roommateMemberId),
                LocalDateTime.of(2026, 7, 12, 16, 0),
                LocalDateTime.of(2026, 7, 12, 18, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(masterCalendar));
        given(repeatRoommateCalendarRepository.findOneByRoommateCalendar(masterCalendar)).willReturn(Optional.of(repeatCalendar));
        given(roommateCalendarCategoryRepository.save(any(RoommateCalendarCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .willAnswer(invocation -> savedCalendar(300L, invocation.getArgument(0)));

        // When
        RepeatCalendarModifyDto.Response response = calendarService.modifyRepeat(ownerId, calendarId, myRoommate, request);

        // Then
        assertThat(repeatCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 5, 18, 0));
        assertThat(repeatCalendar.getRepeatType()).isEqualTo(RepeatType.WEEKLY);

        ArgumentCaptor<RoommateCalendar> calendarCaptor = ArgumentCaptor.forClass(RoommateCalendar.class);
        verify(roommateCalendarRepository).save(calendarCaptor.capture());
        assertThat(calendarCaptor.getValue().getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 11, 12, 0));
        assertThat(calendarCaptor.getValue().getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 13, 20, 0));

        ArgumentCaptor<RepeatRoommateCalendar> repeatCaptor = ArgumentCaptor.forClass(RepeatRoommateCalendar.class);
        verify(repeatRoommateCalendarRepository).save(repeatCaptor.capture());
        assertThat(repeatCaptor.getValue().getRoommateCalendar().getId()).isEqualTo(300L);
        assertThat(repeatCaptor.getValue().getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 30, 20, 0));
        assertThat(repeatCaptor.getValue().getRepeatType()).isEqualTo(RepeatType.BI_WEEKLY);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(excludeRoommateCalendarRepository, never()).save(any(ExcludeRoommateCalendar.class));
    }

    @Test
    @DisplayName("격주 반복 일정 중 선택 일자부터 수정하면 기존 반복을 직전 격주 일정 종료일까지 끊는다")
    void modifyRepeatCalendarThisAndFollowingEndsBiWeeklyRepeatAtPreviousOccurrenceEnd() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar masterCalendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "청소",
                "기존 반복",
                "기존 내용",
                LocalDateTime.of(2026, 7, 5, 16, 0),
                LocalDateTime.of(2026, 7, 5, 18, 0)
        );
        RepeatRoommateCalendar repeatCalendar = repeatRoommateCalendar(
                500L,
                masterCalendar,
                LocalDateTime.of(2026, 8, 31, 18, 0),
                RepeatType.BI_WEEKLY
        );
        RepeatCalendarModifyDto.Request request = repeatModifyRequest(
                RepeatCalendarModifyType.THIS_AND_FOLLOWING,
                "수정 반복",
                "수정 내용",
                LocalDateTime.of(2026, 7, 19, 12, 0),
                LocalDateTime.of(2026, 7, 19, 20, 0),
                "공과금",
                LocalDateTime.of(2026, 9, 30, 20, 0),
                RepeatType.WEEKLY,
                List.of(ownerId, roommateMemberId),
                LocalDateTime.of(2026, 7, 19, 16, 0),
                LocalDateTime.of(2026, 7, 19, 18, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(masterCalendar));
        given(repeatRoommateCalendarRepository.findOneByRoommateCalendar(masterCalendar)).willReturn(Optional.of(repeatCalendar));
        given(roommateCalendarCategoryRepository.save(any(RoommateCalendarCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .willAnswer(invocation -> savedCalendar(300L, invocation.getArgument(0)));

        // When
        calendarService.modifyRepeat(ownerId, calendarId, myRoommate, request);

        // Then
        assertThat(repeatCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 5, 18, 0));
        assertThat(repeatCalendar.getRepeatType()).isEqualTo(RepeatType.BI_WEEKLY);
    }

    @Test
    @DisplayName("월간 반복 일정 중 선택 일자부터 수정하면 기존 반복을 직전 월간 일정 종료일까지 끊는다")
    void modifyRepeatCalendarThisAndFollowingEndsMonthlyRepeatAtPreviousOccurrenceEnd() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar masterCalendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "청소",
                "기존 반복",
                "기존 내용",
                LocalDateTime.of(2026, 7, 31, 16, 0),
                LocalDateTime.of(2026, 7, 31, 18, 0)
        );
        RepeatRoommateCalendar repeatCalendar = repeatRoommateCalendar(
                500L,
                masterCalendar,
                LocalDateTime.of(2026, 12, 31, 18, 0),
                RepeatType.MONTHLY
        );
        RepeatCalendarModifyDto.Request request = repeatModifyRequest(
                RepeatCalendarModifyType.THIS_AND_FOLLOWING,
                "수정 반복",
                "수정 내용",
                LocalDateTime.of(2026, 8, 31, 12, 0),
                LocalDateTime.of(2026, 8, 31, 20, 0),
                "공과금",
                LocalDateTime.of(2026, 12, 31, 20, 0),
                RepeatType.MONTHLY,
                List.of(ownerId, roommateMemberId),
                LocalDateTime.of(2026, 8, 31, 16, 0),
                LocalDateTime.of(2026, 8, 31, 18, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(masterCalendar));
        given(repeatRoommateCalendarRepository.findOneByRoommateCalendar(masterCalendar)).willReturn(Optional.of(repeatCalendar));
        given(roommateCalendarCategoryRepository.save(any(RoommateCalendarCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .willAnswer(invocation -> savedCalendar(300L, invocation.getArgument(0)));

        // When
        calendarService.modifyRepeat(ownerId, calendarId, myRoommate, request);

        // Then
        assertThat(repeatCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 31, 18, 0));
        assertThat(repeatCalendar.getRepeatType()).isEqualTo(RepeatType.MONTHLY);
    }

    @Test
    @DisplayName("반복 일정 전체를 수정하면 원래 반복 일자와 요청 일정의 차이를 마스터 일정에 반영한다")
    void modifyRepeatCalendarAllAppliesOccurrenceDeltaToMaster() {
        // Given
        Long calendarId = 100L;
        Long ownerId = 1L;
        Long roommateMemberId = 2L;
        MyRoommate myRoommate = myRoommate(10L, ownerId, roommateMemberId);
        RoommateCalendar masterCalendar = roommateCalendar(
                calendarId,
                myRoommate,
                ownerId,
                "청소",
                "기존 반복",
                "기존 내용",
                LocalDateTime.of(2026, 7, 5, 16, 0),
                LocalDateTime.of(2026, 7, 5, 18, 0)
        );
        RepeatRoommateCalendar repeatCalendar = repeatRoommateCalendar(
                500L,
                masterCalendar,
                LocalDateTime.of(2026, 8, 31, 18, 0),
                RepeatType.WEEKLY
        );
        RepeatCalendarModifyDto.Request request = repeatModifyRequest(
                RepeatCalendarModifyType.ALL,
                "수정 반복",
                "수정 내용",
                LocalDateTime.of(2026, 7, 11, 12, 0),
                LocalDateTime.of(2026, 7, 13, 20, 0),
                "공과금",
                LocalDateTime.of(2026, 9, 30, 20, 0),
                RepeatType.BI_WEEKLY,
                List.of(ownerId, roommateMemberId),
                LocalDateTime.of(2026, 7, 12, 16, 0),
                LocalDateTime.of(2026, 7, 12, 18, 0)
        );

        given(roommateCalendarRepository.findById(calendarId)).willReturn(Optional.of(masterCalendar));
        given(repeatRoommateCalendarRepository.findOneByRoommateCalendar(masterCalendar)).willReturn(Optional.of(repeatCalendar));
        given(roommateCalendarMemberRepository.findByRoommateCalendar(masterCalendar))
                .willReturn(List.of(RoommateCalendarMember.of(masterCalendar, member(ownerId))));

        // When
        RepeatCalendarModifyDto.Response response = calendarService.modifyRepeat(ownerId, calendarId, myRoommate, request);

        // Then
        assertThat(masterCalendar.getTitle()).isEqualTo("수정 반복");
        assertThat(masterCalendar.getContents()).isEqualTo("수정 내용");
        assertThat(masterCalendar.getStartDate()).isEqualTo(LocalDateTime.of(2026, 7, 4, 12, 0));
        assertThat(masterCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 7, 6, 20, 0));
        assertThat(masterCalendar.getRoommateCalendarCategory().getName()).isEqualTo("공과금");
        assertThat(repeatCalendar.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 30, 20, 0));
        assertThat(repeatCalendar.getRepeatType()).isEqualTo(RepeatType.BI_WEEKLY);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(roommateCalendarRepository, never()).save(any(RoommateCalendar.class));
        verify(repeatRoommateCalendarRepository, never()).save(any(RepeatRoommateCalendar.class));
        verify(excludeRoommateCalendarRepository, never()).save(any(ExcludeRoommateCalendar.class));
    }

    private CalendarDto.Request calendarRequest(
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String categoryName,
            List<Long> memberIds
    ) {
        CalendarDto.Request request = new CalendarDto.Request();
        request.setCalendar(calendarInfo(title, contents, startDate, endDate));
        request.setCategoryName(categoryName);
        request.setMemberIds(memberIds);
        return request;
    }

    private RepeatCalendarDto.Request repeatCalendarRequest(
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String categoryName,
            LocalDateTime repeatEndDate,
            RepeatType repeatType,
            List<Long> memberIds
    ) {
        RepeatCalendarDto.Request request = new RepeatCalendarDto.Request();
        request.setCalendar(calendarInfo(title, contents, startDate, endDate));
        request.setCategoryName(categoryName);
        request.setRepeatInfo(repeatCalendarInfo(repeatEndDate, repeatType));
        request.setMemberIds(memberIds);
        return request;
    }

    private RepeatCalendarModifyDto.Request repeatModifyRequest(
            RepeatCalendarModifyType modifyType,
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String categoryName,
            LocalDateTime repeatEndDate,
            RepeatType repeatType,
            List<Long> memberIds,
            LocalDateTime originalStartDate,
            LocalDateTime originalEndDate
    ) {
        RepeatCalendarModifyDto.Request request = new RepeatCalendarModifyDto.Request();
        request.setModifyType(modifyType);
        request.setCalendar(calendarInfo(title, contents, startDate, endDate));
        request.setCategoryName(categoryName);
        request.setRepeatInfo(repeatCalendarInfo(repeatEndDate, repeatType));
        request.setMemberIds(memberIds);
        request.setOriginalCalendar(originalCalendar(originalStartDate, originalEndDate));
        return request;
    }

    private CalendarDto.CalendarInfoDto calendarInfo(
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        CalendarDto.CalendarInfoDto calendarInfo = new CalendarDto.CalendarInfoDto();
        calendarInfo.setMyRoommateId(10L);
        calendarInfo.setTitle(title);
        calendarInfo.setContents(contents);
        calendarInfo.setStartDate(startDate);
        calendarInfo.setEndDate(endDate);
        return calendarInfo;
    }

    private RepeatCalendarDto.RepeatCalendarInfo repeatCalendarInfo(LocalDateTime endDate, RepeatType repeatType) {
        RepeatCalendarDto.RepeatCalendarInfo repeatInfo = new RepeatCalendarDto.RepeatCalendarInfo();
        repeatInfo.setEndDate(endDate);
        repeatInfo.setRepeatType(repeatType);
        return repeatInfo;
    }

    private RepeatCalendarModifyDto.OriginalCalendar originalCalendar(LocalDateTime startDate, LocalDateTime endDate) {
        RepeatCalendarModifyDto.OriginalCalendar originalCalendar = new RepeatCalendarModifyDto.OriginalCalendar();
        originalCalendar.setStartDate(startDate);
        originalCalendar.setEndDate(endDate);
        return originalCalendar;
    }

    private DailyCalendarRow dailyCalendarRow(
            Long calendarId,
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String categoryName,
            Long repeatCalendarId,
            LocalDateTime repeatEndDate,
            RepeatType repeatType
    ) {
        return new DailyCalendarRow(
                calendarId,
                title,
                contents,
                startDate,
                endDate,
                categoryName,
                repeatCalendarId,
                repeatEndDate,
                repeatType
        );
    }

    private MonthlyCalendarRow monthlyCalendarRow(
            Long calendarId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long repeatCalendarId,
            LocalDateTime repeatEndDate,
            RepeatType repeatType
    ) {
        return new MonthlyCalendarRow(
                calendarId,
                startDate,
                endDate,
                repeatCalendarId,
                repeatEndDate,
                repeatType
        );
    }

    private MyRoommate myRoommate(Long id, Long requesterId, Long requesteeId) {
        RoommateMatchingRequired matchingRequired = RoommateMatchingRequired.builder()
                .requester(member(requesterId))
                .requestee(member(requesteeId))
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();

        return MyRoommate.builder()
                .id(id)
                .roommateMatchingRequired(matchingRequired)
                .isDeleted(false)
                .build();
    }

    private Member member(Long id) {
        return Member.builder().id(id).build();
    }

    private RoommateCalendar roommateCalendar(
            Long id,
            MyRoommate myRoommate,
            Long ownerId,
            String categoryName,
            String title,
            String contents,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return RoommateCalendar.builder()
                .id(id)
                .myRoommate(myRoommate)
                .member(member(ownerId))
                .roommateCalendarCategory(RoommateCalendarCategory.builder().id(20L).name(categoryName).build())
                .title(title)
                .contents(contents)
                .startDate(startDate)
                .endDate(endDate)
                .isDeleted(false)
                .build();
    }

    private RepeatRoommateCalendar repeatRoommateCalendar(
            Long id,
            RoommateCalendar calendar,
            LocalDateTime endDate,
            RepeatType repeatType
    ) {
        return RepeatRoommateCalendar.builder()
                .id(id)
                .roommateCalendar(calendar)
                .endDate(endDate)
                .repeatType(repeatType)
                .build();
    }

    private RoommateCalendar savedCalendar(Long id, RoommateCalendar calendar) {
        return RoommateCalendar.builder()
                .id(id)
                .myRoommate(calendar.getMyRoommate())
                .member(calendar.getMember())
                .roommateCalendarCategory(calendar.getRoommateCalendarCategory())
                .title(calendar.getTitle())
                .contents(calendar.getContents())
                .startDate(calendar.getStartDate())
                .endDate(calendar.getEndDate())
                .isDeleted(calendar.getIsDeleted())
                .build();
    }
}
