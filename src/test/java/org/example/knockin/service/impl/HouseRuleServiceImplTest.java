package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.example.knockin.mate.dto.HouseRuleDetailDto;
import org.example.knockin.mate.dto.HouseRuleDto;
import org.example.knockin.mate.dto.HouseRuleListDto;
import org.example.knockin.member.entity.Member;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RoommateHouseRule;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.MyRoommateErrorCode;
import org.example.knockin.mate.service.impl.HouseRuleServiceImpl;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.mate.repository.RoommateHouseRuleRepository;
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
@DisplayName("하우스룰 서비스")
class HouseRuleServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoommateHouseRuleRepository roommateHouseRuleRepository;

    @InjectMocks
    private HouseRuleServiceImpl houseRuleService;

    @BeforeEach
    void setUp() {
        houseRuleService = new HouseRuleServiceImpl(
                roommateHouseRuleRepository,
                new MemberServiceImpl(memberRepository, null, null, null, null, null, null, null, null, null, null)
        );
    }

    @Test
    @DisplayName("내 룸메이트가 있으면 하우스룰을 저장하고 수정 시간을 반환한다")
    void saveWhenMyRoommateExists() {
        // Given
        Long memberId = 1L;
        Member member = member(memberId);
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L);
        HouseRuleDto.Request request = houseRuleRequest("청소 당번", "매주 일요일에 청소한다");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // When
        HouseRuleDto.Response response = houseRuleService.save(myRoommate, request, memberId);

        // Then
        ArgumentCaptor<RoommateHouseRule> captor = ArgumentCaptor.forClass(RoommateHouseRule.class);
        verify(roommateHouseRuleRepository).save(captor.capture());
        RoommateHouseRule savedRule = captor.getValue();
        assertThat(savedRule.getMember()).isSameAs(member);
        assertThat(savedRule.getMyRoommate()).isSameAs(myRoommate);
        assertThat(savedRule.getTitle()).isEqualTo("청소 당번");
        assertThat(savedRule.getContents()).isEqualTo("매주 일요일에 청소한다");
        assertThat(savedRule.getIsDeleted()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("하우스룰 저장 시 회원이 없으면 회원 없음 예외를 던지고 저장하지 않는다")
    void saveThrowsWhenMemberDoesNotExist() {
        // Given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> houseRuleService.save(myRoommate(10L, memberId, 2L), houseRuleRequest("제목", "내용"), memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verify(roommateHouseRuleRepository, never()).save(any(RoommateHouseRule.class));
    }

    @Test
    @DisplayName("내 룸메이트가 있으면 삭제되지 않은 하우스룰 목록을 응답 형식으로 반환한다")
    void findListReturnsNotDeletedRules() {
        // Given
        Long memberId = 1L;
        MyRoommate myRoommate = myRoommate(10L, memberId, 2L);
        List<RoommateHouseRule> rules = List.of(
                houseRule(100L, "분리수거", "수요일 밤에 내놓기", myRoommate),
                houseRule(101L, "소등", "자정 이후 거실 소등", myRoommate)
        );
        given(roommateHouseRuleRepository.findByMyRoommateIdAndIsDeleted(10L, false)).willReturn(rules);

        // When
        List<HouseRuleListDto.Response> responses = houseRuleService.findList(myRoommate);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(100L);
        assertThat(responses.get(0).getTitle()).isEqualTo("분리수거");
        assertThat(responses.get(0).getContents()).isEqualTo("수요일 밤에 내놓기");
        assertThat(responses.get(1).getId()).isEqualTo(101L);
        verify(roommateHouseRuleRepository).findByMyRoommateIdAndIsDeleted(10L, false);
    }

    @Test
    @DisplayName("요청자가 하우스룰 상세를 조회하면 하우스룰 상세 정보를 반환한다")
    void findHouseRuleDetailReturnsRuleForRequester() {
        // Given
        Long memberId = 1L;
        RoommateHouseRule rule = houseRule(100L, "환기", "아침마다 창문 열기", myRoommate(10L, memberId, 2L));
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.of(rule));

        // When
        HouseRuleDetailDto.Response response = houseRuleService.findDetail(memberId, 100L);

        // Then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("환기");
        assertThat(response.getContents()).isEqualTo("아침마다 창문 열기");
    }

    @Test
    @DisplayName("상대방이 하우스룰 상세를 조회하면 하우스룰 상세 정보를 반환한다")
    void findHouseRuleDetailReturnsRuleForRequestee() {
        // Given
        Long requesteeId = 2L;
        RoommateHouseRule rule = houseRule(100L, "환기", "아침마다 창문 열기", myRoommate(10L, 1L, requesteeId));
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.of(rule));

        // When
        HouseRuleDetailDto.Response response = houseRuleService.findDetail(requesteeId, 100L);

        // Then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("환기");
        assertThat(response.getContents()).isEqualTo("아침마다 창문 열기");
    }

    @Test
    @DisplayName("하우스룰 상세 조회 시 하우스룰이 없으면 하우스룰 없음 예외를 던진다")
    void findHouseRuleDetailThrowsWhenRuleDoesNotExist() {
        // Given
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> houseRuleService.findDetail(1L, 100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.HOUSE_RULE_NOT_FOUND));
    }

    @Test
    @DisplayName("룸메이트 관계자가 아니면 하우스룰 상세 조회를 거부한다")
    void findHouseRuleDetailThrowsWhenMemberIsNotRoommateParticipant() {
        // Given
        RoommateHouseRule rule = houseRule(100L, "환기", "아침마다 창문 열기", myRoommate(10L, 1L, 2L));
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.of(rule));

        // When & Then
        assertThatThrownBy(() -> houseRuleService.findDetail(999L, 100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.HOUSE_RULE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("룸메이트 관계자가 하우스룰을 수정하면 제목과 내용을 변경하고 수정 시간을 반환한다")
    void modifyHouseRuleChangesTitleAndContentsForParticipant() {
        // Given
        Long memberId = 1L;
        RoommateHouseRule rule = houseRule(100L, "기존 제목", "기존 내용", myRoommate(10L, memberId, 2L));
        HouseRuleDto.Request request = houseRuleRequest("변경 제목", "변경 내용");
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.of(rule));

        // When
        HouseRuleDto.Response response = houseRuleService.modify(memberId, 100L, request);

        // Then
        assertThat(rule.getTitle()).isEqualTo("변경 제목");
        assertThat(rule.getContents()).isEqualTo("변경 내용");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("룸메이트 관계자가 하우스룰을 삭제하면 삭제 처리하고 수정 시간을 반환한다")
    void deleteForParticipant() {
        // Given
        Long memberId = 1L;
        RoommateHouseRule rule = houseRule(100L, "소등", "자정 이후 거실 소등", myRoommate(10L, memberId, 2L));
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.of(rule));

        // When
        HouseRuleDto.Response response = houseRuleService.delete(memberId, 100L);

        // Then
        assertThat(rule.getIsDeleted()).isTrue();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("룸메이트 관계자가 아니면 하우스룰을 삭제하지 않고 접근 거부 예외를 던진다")
    void deleteThrowsWhenMemberIsNotRoommateParticipant() {
        // Given
        RoommateHouseRule rule = houseRule(100L, "소등", "자정 이후 거실 소등", myRoommate(10L, 1L, 2L));
        given(roommateHouseRuleRepository.findWithFetchedById(100L)).willReturn(Optional.of(rule));

        // When & Then
        assertThatThrownBy(() -> houseRuleService.delete(999L, 100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.HOUSE_RULE_ACCESS_DENIED));
        assertThat(rule.getIsDeleted()).isFalse();
    }

    private HouseRuleDto.Request houseRuleRequest(String title, String contents) {
        HouseRuleDto.Request request = new HouseRuleDto.Request();
        request.setTitle(title);
        request.setContents(contents);
        return request;
    }

    private Member member(Long id) {
        return Member.builder().id(id).build();
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

    private RoommateHouseRule houseRule(Long id, String title, String contents, MyRoommate myRoommate) {
        return RoommateHouseRule.builder()
                .id(id)
                .member(myRoommate.getRoommateMatchingRequired().getRequester())
                .myRoommate(myRoommate)
                .title(title)
                .contents(contents)
                .isDeleted(false)
                .build();
    }
}
