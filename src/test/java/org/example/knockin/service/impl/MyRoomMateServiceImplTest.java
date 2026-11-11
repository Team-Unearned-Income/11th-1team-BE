package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.knockin.chat.service.impl.ChattingScoreServiceImpl;
import org.example.knockin.mate.dto.CalendarDto;
import org.example.knockin.mate.dto.CalendarEditDto;
import org.example.knockin.mate.dto.HouseRuleDto;
import org.example.knockin.mate.dto.HouseRuleListDto;
import org.example.knockin.mate.dto.MyRoommateCardDto;
import org.example.knockin.mate.dto.MyRoommateDailyCalendarListDto;
import org.example.knockin.mate.dto.MyRoommateMonthlyCalendarListDto;
import org.example.knockin.mate.dto.RepeatCalendarModifyDto;
import org.example.knockin.mate.dto.RepeatCalendarModifyType;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.mate.dto.MyRoommateDto;
import org.example.knockin.mate.dto.RepeatCalendarDto;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RepeatType;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.mate.entity.RoommateScore;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.MyRoommateErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.mate.service.impl.CalendarServiceImpl;
import org.example.knockin.mate.service.impl.HouseRuleServiceImpl;
import org.example.knockin.member.repository.row.ChattingRoomBasicInfoRow;
import org.example.knockin.mate.repository.MyRoommateRepository;
import org.example.knockin.mate.service.impl.MyRoomMateServiceImpl;
import org.example.knockin.mate.service.impl.MyRoommateScoreServiceImpl;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberPrivacyServiceImpl;
import org.example.knockin.util.service.RoommateScoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("내 룸메이트 서비스")
class MyRoomMateServiceImplTest {

    @Mock
    private MyRoommateRepository myRoommateRepository;

    @Mock
    private RoommateScoreService roommateScoreService;

    @Mock
    private MemberPrivacyServiceImpl memberPrivacyService;

    @Mock
    private BasicInformationServiceImpl basicInformationService;

    @Mock
    private MyRoommateScoreServiceImpl myRoommateScoreService;

    @Mock
    private CalendarServiceImpl calendarService;

    @Mock
    private HouseRuleServiceImpl houseRuleService;

    @Mock
    private ChattingScoreServiceImpl chattingScoreService;

    @InjectMocks
    private MyRoomMateServiceImpl myRoomMateService;

    @Test
    @DisplayName("룸메이트를 저장하면 채팅 시점의 양방향 궁합 점수를 룸메이트 점수로 전파한다")
    void savePropagatesChattingScoresToRoommateScores() {
        // Given
        Member requester = Member.builder().id(1L).build();
        Member requestee = Member.builder().id(2L).build();
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .id(10L)
                .requesterId(requester.getId())
                .requesteeId(requestee.getId())
                .build();
        ChattingRoom chattingRoom = ChattingRoom.builder()
                .id(100L)
                .chattingRequired(chattingRequired)
                .build();
        RoommateMatchingRequired matchingRequired = RoommateMatchingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .chattingRoomId(chattingRoom.getId())
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();
        ChattingScore requesterScore = ChattingScore.builder().id(201L).score(80).build();
        ChattingScore requesteeScore = ChattingScore.builder().id(202L).score(70).build();

        when(myRoommateRepository.save(any(MyRoommate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chattingScoreService.findByChattingRoomId(100L))
                .thenReturn(List.of(requesterScore, requesteeScore));

        // When
        MyRoommate savedMyRoommate = myRoomMateService.save(matchingRequired);

        // Then
        assertThat(savedMyRoommate.getRoommateMatchingRequired()).isSameAs(matchingRequired);
        verify(chattingScoreService).findByChattingRoomId(100L);
        verify(myRoommateScoreService).saveAll(org.mockito.ArgumentMatchers.argThat(roommateScores ->
                roommateScores.size() == 2
                        && roommateScores.stream().allMatch(score -> score.getMyRoommate() == savedMyRoommate)
                        && roommateScores.get(0).getChattingScoreId().equals(requesterScore.getId())
                        && roommateScores.get(1).getChattingScoreId().equals(requesteeScore.getId())
        ));
        verifyNoInteractions(roommateScoreService);
    }

    @Test
    @DisplayName("룸메이트가 존재하면 true를 반환한다")
    void isExistRoomMateTest() {
        // given
        Member member = mock(Member.class);
        given(myRoommateRepository.isExistRoomMate(member)).willReturn(true);

        // when
        boolean result = myRoomMateService.isExistRoomMate(member);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("내 룸메이트가 있으면 상대 기본 정보와 채팅방과 궁합 점수를 반환한다")
    void findMyRoommateReturnsOpponentInfoChatRoomAndScore() {
        // Given
        Long memberId = 1L;
        Long opponentId = 2L;
        LocalDate opponentBirth = LocalDate.of(2000, 1, 1);
        MyRoommate myRoommate = myRoommate(10L, memberId, opponentId, 100L);
        ChattingScore chattingScore = ChattingScore.builder().id(203L).score(92).build();
        RoommateScore roommateScore = RoommateScore.builder().chattingScoreId(chattingScore.getId()).build();
        ChattingRoomBasicInfoRow basicInfoRow = new ChattingRoomBasicInfoRow(
                opponentId,
                "상대방",
                opponentBirth,
                Gender.FEMALE,
                "opponent-profile.jpg"
        );

        when(myRoommateRepository.findWithRequiredByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(basicInformationService.findChattingRoomBasicInfoRowByMemberId(opponentId)).thenReturn(basicInfoRow);
        when(myRoommateScoreService.findByRoommateIdAndChattingScoreId(10L, 203L)).thenReturn(Optional.of(roommateScore));
        when(chattingScoreService.findByChattingRoomIdAndMemberId(100L, memberId)).thenReturn(Optional.of(chattingScore));

        // When
        MyRoommateCardDto.Response response = myRoomMateService.findMyRoommate(memberId);

        // Then
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getChatRoomId()).isEqualTo(100L);
        assertThat(response.getScore()).isEqualTo(92);
        assertThat(response.getMyRoommateInfo().getMemberId()).isEqualTo(opponentId);
        assertThat(response.getMyRoommateInfo().getMemberName()).isEqualTo("상대방");
        assertThat(response.getMyRoommateInfo().getMemberAge()).isEqualTo(DateUtils.calculateAge(opponentBirth));
        assertThat(response.getMyRoommateInfo().getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getMyRoommateInfo().getMemberProfileImageUrl()).isEqualTo("opponent-profile.jpg");
        verify(basicInformationService).findChattingRoomBasicInfoRowByMemberId(opponentId);
        verify(myRoommateScoreService).findByRoommateIdAndChattingScoreId(10L, 203L);
        verify(roommateScoreService, never()).calculateSimpleScore(memberId, opponentId);
    }

    @Test
    @DisplayName("저장된 룸메이트 점수가 없으면 현재 프로필 궁합 점수를 반환한다")
    void findMyRoommateFallsBackToCurrentScoreWhenSnapshotDoesNotExist() {
        // Given
        Long memberId = 1L;
        Long opponentId = 2L;
        MyRoommate myRoommate = myRoommate(10L, memberId, opponentId, 100L);
        ChattingRoomBasicInfoRow basicInfoRow = new ChattingRoomBasicInfoRow(
                opponentId,
                "상대방",
                LocalDate.of(2000, 1, 1),
                Gender.FEMALE,
                "opponent-profile.jpg"
        );

        when(myRoommateRepository.findWithRequiredByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(basicInformationService.findChattingRoomBasicInfoRowByMemberId(opponentId)).thenReturn(basicInfoRow);
        when(chattingScoreService.findByChattingRoomIdAndMemberId(100L, memberId)).thenReturn(Optional.empty());
        when(roommateScoreService.calculateSimpleScore(memberId, opponentId)).thenReturn(76);

        // When
        MyRoommateCardDto.Response response = myRoomMateService.findMyRoommate(memberId);

        // Then
        assertThat(response.getScore()).isEqualTo(76);
        verify(roommateScoreService).calculateSimpleScore(memberId, opponentId);
    }

    @Test
    @DisplayName("내 룸메이트가 없으면 룸메이트 없음 예외를 던진다")
    void findMyRoommateThrowsWhenMyRoommateDoesNotExist() {
        // Given
        Long memberId = 1L;
        when(myRoommateRepository.findWithRequiredByMemberId(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> myRoomMateService.findMyRoommate(memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.NOT_FOUND));
        verifyNoInteractions(basicInformationService, myRoommateScoreService, roommateScoreService);
    }

    @Test
    @DisplayName("상대 회원의 기본 정보가 없으면 기본 정보 없음 예외를 던진다")
    void findMyRoommateThrowsWhenOpponentBasicInformationDoesNotExist() {
        // Given
        Long memberId = 1L;
        Long opponentId = 2L;
        MyRoommate myRoommate = myRoommate(10L, memberId, opponentId, 100L);
        when(myRoommateRepository.findWithRequiredByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(basicInformationService.findChattingRoomBasicInfoRowByMemberId(opponentId))
                .thenThrow(new BusinessException(MemberErrorCode.BASIC_INFO_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> myRoomMateService.findMyRoommate(memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.BASIC_INFO_NOT_FOUND));
        verifyNoInteractions(myRoommateScoreService, roommateScoreService);
    }

    @Test
    @DisplayName("내 룸메이트를 삭제하면 룸메이트를 삭제 처리하고 양쪽 회원의 공개범위를 공개로 변경한다")
    void deleteMyRoommateSoftDeletesMyRoommateAndChangesBothMembersPrivacyPublic() {
        // Given
        Long myRoommateId = 10L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(myRoommateId, requesterId, requesteeId, 100L);
        MemberPrivacy requesterPrivacy = MemberPrivacy.builder()
                .type(MemberPrivacyType.PRIVATE)
                .build();
        MemberPrivacy requesteePrivacy = MemberPrivacy.builder()
                .type(MemberPrivacyType.PRIVATE)
                .build();

        when(myRoommateRepository.findWithRequiredByMemberId(requesterId)).thenReturn(Optional.of(myRoommate));
        when(memberPrivacyService.findByMemberId(requesterId)).thenReturn(List.of(requesterPrivacy));
        when(memberPrivacyService.findByMemberId(requesteeId)).thenReturn(List.of(requesteePrivacy));

        // When
        MyRoommateDto.Response response = myRoomMateService.deleteMyRoommate(myRoommateId, requesterId);

        // Then
        assertThat(myRoommate.getIsDeleted()).isTrue();
        assertThat(requesterPrivacy.getType()).isEqualTo(MemberPrivacyType.PUBLIC);
        assertThat(requesteePrivacy.getType()).isEqualTo(MemberPrivacyType.PUBLIC);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(memberPrivacyService).findByMemberId(requesterId);
        verify(memberPrivacyService).findByMemberId(requesteeId);
    }

    @Test
    @DisplayName("요청받은 회원이 룸메이트를 삭제해도 양쪽 회원의 공개범위를 공개로 변경한다")
    void deleteMyRoommateByRequesteeChangesBothMembersPrivacyPublic() {
        // Given
        Long myRoommateId = 10L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        MyRoommate myRoommate = myRoommate(myRoommateId, requesterId, requesteeId, 100L);
        MemberPrivacy requesterPrivacy = MemberPrivacy.builder()
                .type(MemberPrivacyType.PRIVATE)
                .build();
        MemberPrivacy requesteePrivacy = MemberPrivacy.builder()
                .type(MemberPrivacyType.PRIVATE)
                .build();

        when(myRoommateRepository.findWithRequiredByMemberId(requesteeId)).thenReturn(Optional.of(myRoommate));
        when(memberPrivacyService.findByMemberId(requesterId)).thenReturn(List.of(requesterPrivacy));
        when(memberPrivacyService.findByMemberId(requesteeId)).thenReturn(List.of(requesteePrivacy));

        // When
        myRoomMateService.deleteMyRoommate(myRoommateId, requesteeId);

        // Then
        assertThat(myRoommate.getIsDeleted()).isTrue();
        assertThat(requesterPrivacy.getType()).isEqualTo(MemberPrivacyType.PUBLIC);
        assertThat(requesteePrivacy.getType()).isEqualTo(MemberPrivacyType.PUBLIC);
        verify(memberPrivacyService).findByMemberId(requesterId);
        verify(memberPrivacyService).findByMemberId(requesteeId);
    }

    @Test
    @DisplayName("삭제하려는 룸메이트 ID가 내 룸메이트와 다르면 삭제 처리와 공개범위 변경을 하지 않는다")
    void deleteMyRoommateRejectsDifferentMyRoommateId() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        when(myRoommateRepository.findWithRequiredByMemberId(memberId)).thenReturn(Optional.of(myRoommate));

        // When & Then
        assertThatThrownBy(() -> myRoomMateService.deleteMyRoommate(999L, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        assertThat(myRoommate.getIsDeleted()).isFalse();
        verifyNoInteractions(memberPrivacyService);
    }

    @Test
    @DisplayName("삭제할 내 룸메이트가 없으면 룸메이트 없음 예외를 던지고 공개범위를 변경하지 않는다")
    void deleteMyRoommateThrowsWhenMyRoommateDoesNotExist() {
        // Given
        Long memberId = 1L;
        when(myRoommateRepository.findWithRequiredByMemberId(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> myRoomMateService.deleteMyRoommate(10L, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.NOT_FOUND));
        verifyNoInteractions(memberPrivacyService);
    }

    @Test
    @DisplayName("하우스룰 저장 시 내 룸메이트를 조회해 하우스룰 서비스에 전달한다")
    void saveHouseRuleFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        HouseRuleDto.Request request = houseRuleRequest("청소", "매주 일요일 청소");
        HouseRuleDto.Response expected = HouseRuleDto.Response.builder()
                .updatedAt(LocalDateTime.of(2026, 7, 9, 12, 0))
                .build();

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(houseRuleService.save(myRoommate, request, memberId)).thenReturn(expected);

        // When
        HouseRuleDto.Response response = myRoomMateService.saveHouseRule(request, memberId);

        // Then
        assertThat(response).isSameAs(expected);
        verify(houseRuleService).save(myRoommate, request, memberId);
    }

    @Test
    @DisplayName("하우스룰 저장 시 내 룸메이트가 없으면 하우스룰 서비스에 위임하지 않고 예외를 던진다")
    void saveHouseRuleThrowsWhenMyRoommateDoesNotExist() {
        // Given
        Long memberId = 1L;
        HouseRuleDto.Request request = houseRuleRequest("청소", "매주 일요일 청소");
        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> myRoomMateService.saveHouseRule(request, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.NOT_FOUND));
        verifyNoInteractions(houseRuleService);
    }

    @Test
    @DisplayName("하우스룰 목록 조회 시 내 룸메이트를 조회해 하우스룰 서비스에 전달한다")
    void findHouseRuleListFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        List<HouseRuleListDto.Response> expected = List.of(HouseRuleListDto.Response.builder()
                .id(1L)
                .title("청소")
                .contents("매주 일요일 청소")
                .build());

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(houseRuleService.findList(myRoommate)).thenReturn(expected);

        // When
        List<HouseRuleListDto.Response> response = myRoomMateService.findHouseRuleList(memberId);

        // Then
        assertThat(response).isSameAs(expected);
        verify(houseRuleService).findList(myRoommate);
    }

    @Test
    @DisplayName("일반 일정 저장 시 내 룸메이트를 조회해 캘린더 서비스에 전달한다")
    void saveBasicCalendarFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        CalendarDto.Request request = calendarRequest("장보기", "저녁 재료", List.of(memberId, 2L));
        CalendarDto.Response expected = CalendarDto.Response.builder()
                .updatedAt(LocalDateTime.of(2026, 7, 9, 12, 0))
                .build();

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(calendarService.saveBasic(memberId, myRoommate, request)).thenReturn(expected);

        // When
        CalendarDto.Response response = myRoomMateService.saveBasicCalendar(memberId, request);

        // Then
        assertThat(response).isSameAs(expected);
        verify(calendarService).saveBasic(memberId, myRoommate, request);
    }

    @Test
    @DisplayName("일반 일정 저장 시 내 룸메이트가 없으면 캘린더 서비스에 위임하지 않고 예외를 던진다")
    void saveBasicCalendarThrowsWhenMyRoommateDoesNotExist() {
        // Given
        Long memberId = 1L;
        CalendarDto.Request request = calendarRequest("장보기", "저녁 재료", List.of(memberId));
        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> myRoomMateService.saveBasicCalendar(memberId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.NOT_FOUND));
        verifyNoInteractions(calendarService);
    }

    @Test
    @DisplayName("특정일 일정 조회 시 내 룸메이트를 조회해 캘린더 서비스에 전달한다")
    void findDailyCalendarListFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        MyRoommateDailyCalendarListDto.Response expected = MyRoommateDailyCalendarListDto.Response.builder()
                .targetDay(LocalDate.of(2026, 7, 12))
                .calendars(List.of())
                .build();

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(calendarService.findDailyList(myRoommate, 2026, 7, 12)).thenReturn(expected);

        // When
        MyRoommateDailyCalendarListDto.Response response = myRoomMateService.findDailyCalendarList(memberId, 2026, 7, 12);

        // Then
        assertThat(response).isSameAs(expected);
        verify(calendarService).findDailyList(myRoommate, 2026, 7, 12);
    }

    @Test
    @DisplayName("월별 일정 조회 시 내 룸메이트를 조회해 캘린더 서비스에 전달한다")
    void findMyMonthlyCalendarListFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        MyRoommateMonthlyCalendarListDto.Response expected = MyRoommateMonthlyCalendarListDto.Response.builder()
                .calendarDays(List.of())
                .build();

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(calendarService.findMyMonthlyList(myRoommate, 2026, 7)).thenReturn(expected);

        // When
        MyRoommateMonthlyCalendarListDto.Response response = myRoomMateService.findMyMonthlyCalendarList(memberId, 2026, 7);

        // Then
        assertThat(response).isSameAs(expected);
        verify(calendarService).findMyMonthlyList(myRoommate, 2026, 7);
    }

    @Test
    @DisplayName("캘린더 편집 폼 조회 시 내 룸메이트를 조회해 캘린더 서비스에 전달한다")
    void getRoommateCalendarEditFormFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        CalendarEditDto.Response expected = CalendarEditDto.Response.builder()
                .repeatType(List.of(RepeatType.WEEKLY))
                .members(List.of())
                .categoryNames(List.of("청소"))
                .build();

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(calendarService.getEditForm(memberId, myRoommate)).thenReturn(expected);

        // When
        CalendarEditDto.Response response = myRoomMateService.getRoommateCalendarEditForm(memberId);

        // Then
        assertThat(response).isSameAs(expected);
        verify(calendarService).getEditForm(memberId, myRoommate);
    }

    @Test
    @DisplayName("반복 일정 수정 시 내 룸메이트를 조회해 캘린더 서비스에 전달한다")
    void modifyRepeatCalendarFindsMyRoommateAndDelegates() {
        // Given
        Long memberId = 1L;
        Long calendarId = 100L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L, 100L);
        RepeatCalendarModifyDto.Request request = repeatModifyRequest(List.of(memberId, 2L));
        RepeatCalendarModifyDto.Response expected = RepeatCalendarModifyDto.Response.builder()
                .updatedAt(LocalDateTime.of(2026, 7, 9, 12, 0))
                .build();

        when(myRoommateRepository.findWithRequiredAndMembersByMemberId(memberId)).thenReturn(Optional.of(myRoommate));
        when(calendarService.modifyRepeat(memberId, calendarId, myRoommate, request)).thenReturn(expected);

        // When
        RepeatCalendarModifyDto.Response response = myRoomMateService.modifyRepeatCalendar(memberId, calendarId, request);

        // Then
        assertThat(response).isSameAs(expected);
        verify(calendarService).modifyRepeat(memberId, calendarId, myRoommate, request);
    }

    @Test
    @DisplayName("카테고리명 조회 시 캘린더 서비스의 카테고리명을 반환한다")
    void findCategoryNamesDelegatesToCalendarService() {
        // Given
        List<String> expected = List.of("청소", "공과금", "기타");
        when(calendarService.findCategoryNames()).thenReturn(expected);

        // When
        List<String> response = myRoomMateService.findCategoryNames();

        // Then
        assertThat(response).isSameAs(expected);
        verify(calendarService).findCategoryNames();
    }

    private HouseRuleDto.Request houseRuleRequest(String title, String contents) {
        HouseRuleDto.Request request = new HouseRuleDto.Request();
        request.setTitle(title);
        request.setContents(contents);
        return request;
    }

    private CalendarDto.Request calendarRequest(String title, String contents, List<Long> memberIds) {
        CalendarDto.Request request = new CalendarDto.Request();
        request.setCalendar(CalendarDto.CalendarInfoDto.builder()
                .myRoommateId(10L)
                .title(title)
                .contents(contents)
                .startDate(LocalDateTime.of(2026, 7, 12, 9, 0))
                .endDate(LocalDateTime.of(2026, 7, 12, 10, 0))
                .build());
        request.setCategoryName("청소");
        request.setMemberIds(memberIds);
        return request;
    }

    private RepeatCalendarModifyDto.Request repeatModifyRequest(List<Long> memberIds) {
        RepeatCalendarModifyDto.OriginalCalendar originalCalendar = new RepeatCalendarModifyDto.OriginalCalendar();
        originalCalendar.setStartDate(LocalDateTime.of(2026, 7, 12, 9, 0));
        originalCalendar.setEndDate(LocalDateTime.of(2026, 7, 12, 10, 0));

        RepeatCalendarModifyDto.Request request = new RepeatCalendarModifyDto.Request();
        request.setCalendar(calendarRequest("청소", "거실 청소", memberIds).getCalendar());
        request.setCategoryName("청소");
        request.setRepeatInfo(RepeatCalendarDto.RepeatCalendarInfo.builder()
                .endDate(LocalDateTime.of(2026, 8, 12, 10, 0))
                .repeatType(RepeatType.WEEKLY)
                .build());
        request.setMemberIds(memberIds);
        request.setModifyType(RepeatCalendarModifyType.THIS);
        request.setOriginalCalendar(originalCalendar);
        return request;
    }

    private MyRoommate myRoommate(Long myRoommateId, Long requesterId, Long requesteeId, Long chatRoomId) {
        Member requester = Member.builder().id(requesterId).build();
        Member requestee = Member.builder().id(requesteeId).build();
        ChattingRoom chattingRoom = ChattingRoom.builder()
                .id(chatRoomId)
                .build();
        RoommateMatchingRequired matchingRequired = RoommateMatchingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .chattingRoomId(chattingRoom.getId())
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();

        return MyRoommate.builder()
                .id(myRoommateId)
                .roommateMatchingRequired(matchingRequired)
                .isDeleted(false)
                .build();
    }
}
