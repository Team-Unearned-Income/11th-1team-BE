package org.example.knockin.service.impl;

import org.example.knockin.member.dto.BoMemberDetailDto;
import org.example.knockin.mate.service.impl.RoommateMatchingRequiredServiceImpl;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import java.time.LocalDateTime;
import org.example.knockin.member.dto.BoMemberListDto;
import org.example.knockin.member.dto.FcmDto;
import org.example.knockin.member.dto.MyAccountDto;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.DevicePlatform;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.member.entity.State;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.StateRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.room.repository.RoomProfileRepository;
import org.example.knockin.global.auth.service.Oauth2DeleteFactory;
import org.example.knockin.global.auth.service.Oauth2DeleteService;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.verification.entity.LoginProviderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 테스트")
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private Oauth2DeleteFactory oauth2DeleteFactory;

    @Mock
    private MemberLifePatternRepository memberLifePatternRepository;

    @Mock
    private BasicInformationRepository basicInformationRepository;

    @Mock
    private RoomProfileRepository roomProfileRepository;

    @Mock
    private AlarmSettingRepository alarmSettingRepository;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private RoommateMatchingRequiredServiceImpl roommateMatchingRequiredService;

    @Mock
    private org.example.knockin.chat.service.impl.ChattingRequiredServiceImpl chattingRequiredService;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void hardDeleteUsesOnlyLockedTargetsAndCleansAlarmsFirst() {
        var cutoff = LocalDateTime.of(2026, 11, 8, 0, 0);
        var candidates = List.of(1L, 2L);
        var targets = List.of(2L);
        given(memberRepository.findDeletionTargetIdsForUpdate(candidates, cutoff)).willReturn(targets);
        List<Long> requestIds = List.of(10L);
        List<Long> roomIds = List.of(20L);
        List<Long> scoreIds = List.of(30L, 31L);
        given(chattingRequiredService.findIdsForMembers(targets)).willReturn(requestIds);
        given(chattingRequiredService.findRoomIdsByRequestIds(requestIds)).willReturn(roomIds);
        given(chattingRequiredService.findScoreIdsByRequestIds(requestIds)).willReturn(scoreIds);

        memberService.hardDeleteMember(candidates, cutoff);

        var order = inOrder(memberRepository, alarmService, roommateMatchingRequiredService, chattingRequiredService);
        order.verify(memberRepository).findDeletionTargetIdsForUpdate(candidates, cutoff);
        order.verify(chattingRequiredService).findIdsForMembers(targets);
        order.verify(chattingRequiredService).findRoomIdsByRequestIds(requestIds);
        order.verify(chattingRequiredService).findScoreIdsByRequestIds(requestIds);
        order.verify(alarmService).deleteForMembers(targets, requestIds, roomIds);
        order.verify(roommateMatchingRequiredService).deleteForMembers(targets, roomIds, scoreIds);
        order.verify(chattingRequiredService).deleteForMembers(targets);
        order.verify(memberRepository).deleteMembersByIds(targets);
    }

    @Test
    void hardDeleteDoesNothingWhenNoMembersRemainEligible() {
        var cutoff = LocalDateTime.of(2026, 11, 8, 0, 0);
        given(memberRepository.findDeletionTargetIdsForUpdate(List.of(1L), cutoff)).willReturn(List.of());

        memberService.hardDeleteMember(List.of(1L), cutoff);

        verifyNoInteractions(alarmService, roommateMatchingRequiredService, chattingRequiredService);
        verify(memberRepository, never()).deleteMembersByIds(any());
    }

    @Test
    void hardDeleteDoesNotDeleteMembersWhenAlarmCleanupFails() {
        var cutoff = LocalDateTime.of(2026, 11, 8, 0, 0);
        var targets = List.of(1L);
        given(memberRepository.findDeletionTargetIdsForUpdate(targets, cutoff)).willReturn(targets);
        doThrow(new IllegalStateException("cleanup failed")).when(alarmService).deleteForMembers(targets, List.of(), List.of());

        assertThatThrownBy(() -> memberService.hardDeleteMember(targets, cutoff))
                .isInstanceOf(IllegalStateException.class);

        verify(memberRepository, never()).deleteMembersByIds(any());
    }

    @Test
    void hardDeleteDoesNotDeleteMembersWhenRoommateCleanupFails() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 11, 8, 0, 0);
        List<Long> targets = List.of(1L);
        given(memberRepository.findDeletionTargetIdsForUpdate(targets, cutoff)).willReturn(targets);
        doThrow(new IllegalStateException("cleanup failed")).when(roommateMatchingRequiredService).deleteForMembers(targets, List.of(), List.of());

        assertThatThrownBy(() -> memberService.hardDeleteMember(targets, cutoff))
                .isInstanceOf(IllegalStateException.class);

        verify(memberRepository, never()).deleteMembersByIds(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("소셜 연결 해제 성공 여부와 관계없이 회원을 탈퇴 처리한다")
    void deletesMemberRegardlessOfUnlinkResult(boolean unlinked) {
        Member member = memberForDeletion(LoginProviderType.KAKAO);
        Oauth2DeleteService unlinkService = mock(Oauth2DeleteService.class);
        given(oauth2DeleteFactory.getDeleteService(LoginProviderType.KAKAO)).willReturn(unlinkService);
        given(unlinkService.requestUnlink("provider-id")).willReturn(unlinked);

        var response = memberService.deleteMember("provider-id", LoginProviderType.KAKAO);

        assertThat(member.isDelete()).isTrue();
        assertThat(member.getDeletedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(unlinkService).requestUnlink("provider-id");
    }

    @Test
    @DisplayName("소셜 연결 해제 호출에서 예외가 발생해도 회원을 탈퇴 처리한다")
    void deletesMemberWhenUnlinkThrows() {
        Member member = memberForDeletion(LoginProviderType.KAKAO);
        Oauth2DeleteService unlinkService = mock(Oauth2DeleteService.class);
        given(oauth2DeleteFactory.getDeleteService(LoginProviderType.KAKAO)).willReturn(unlinkService);
        given(unlinkService.requestUnlink("provider-id")).willThrow(new IllegalStateException("upstream failure"));

        memberService.deleteMember("provider-id", LoginProviderType.KAKAO);

        assertThat(member.isDelete()).isTrue();
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Apple 연결 해제 서비스가 없어도 회원을 탈퇴 처리한다")
    void deletesAppleMemberWithoutUnlinkService() {
        Member member = memberForDeletion(LoginProviderType.APPLE);
        given(oauth2DeleteFactory.getDeleteService(LoginProviderType.APPLE))
                .willThrow(new IllegalArgumentException("지원하지 않는 로그인 제공자입니다: APPLE"));

        memberService.deleteMember("provider-id", LoginProviderType.APPLE);

        assertThat(member.isDelete()).isTrue();
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원 조회 실패는 무시하지 않고 연결 해제도 호출하지 않는다")
    void rejectsDeletionForMissingMember() {
        given(memberRepository.findMemberByProvider("missing", LoginProviderType.APPLE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember("missing", LoginProviderType.APPLE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
        verifyNoInteractions(oauth2DeleteFactory);
    }

    private Member memberForDeletion(LoginProviderType providerType) {
        Member member = Member.builder().id(1L).providerId("provider-id").providerType(providerType).build();
        given(memberRepository.findMemberByProvider("provider-id", providerType)).willReturn(Optional.of(member));
        return member;
    }

    @Test
    @DisplayName("백오피스 회원 목록 조회 성공 테스트")
    void findBackOfficeMemberListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoMemberListDto.Response.MemberInfo info = new BoMemberListDto.Response.MemberInfo();
        info.setId(1L);
        info.setName("홍길동");
        info.setEmail("test@test.com");
        BoMemberListDto.Request request = new BoMemberListDto.Request();

        given(memberRepository.findBackOfficeMemberList(pageable, request)).willReturn(List.of(info));

        // when
        BoMemberListDto.Response response = memberService.findBackOfficeMemberList(pageable, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMemberInfoList()).hasSize(1);
        assertThat(response.getMemberInfoList().get(0).getId()).isEqualTo(1L);
        assertThat(response.getMemberInfoList().get(0).getName()).isEqualTo("홍길동");
        verify(memberRepository).findBackOfficeMemberList(pageable, request);
    }

    @Test
    @DisplayName("백오피스 회원 상세 조회 성공 테스트")
    void findBackOfficeMemberSuccessTest() {
        // given
        Long memberId = 1L;
        BoMemberDetailDto.Response detail = new BoMemberDetailDto.Response();
        detail.setId(memberId);
        detail.setName("홍길동");

        given(memberRepository.findBackOfficeMember(memberId)).willReturn(detail);

        // when
        BoMemberDetailDto.Response response = memberService.findBackOfficeMember(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(memberId);
        assertThat(response.getName()).isEqualTo("홍길동");
        verify(memberRepository).findBackOfficeMember(memberId);
    }

    @Test
    @DisplayName("회원 상태 수정 성공 테스트")
    void setMemberStateSuccessTest() {
        // given
        Member member = mock(Member.class);
        State state = spy(State.builder().states(MemberState.ACTIVE).build());

        given(stateRepository.findByMember(member)).willReturn(List.of(state));

        // when
        State result = memberService.setMemberState(member, MemberState.INACTIVE, "정지사유");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStates()).isEqualTo(MemberState.INACTIVE);
        verify(stateRepository).findByMember(member);
        verify(state).rejectState("정지사유");
    }

    @Test
    @DisplayName("회원 권한(Role) 수정 성공 테스트")
    void setMemberAuthSuccessTest() {
        // given
        Member member = spy(Member.builder().role(MemberRole.USER).build());

        // when
        Member result = memberService.setMemberAuth(member, MemberRole.ADMIN);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(MemberRole.ADMIN);
        verify(member).changeRole(MemberRole.ADMIN);
    }

    @Test
    @DisplayName("FCM 디바이스 정보를 모두 저장한다")
    void upsertFcmPropsSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();
        FcmDto.Request request = new FcmDto.Request();
        String maxLengthFcmToken = "t".repeat(512);
        request.setDeviceId("550e8400-e29b-41d4-a716-446655440000");
        request.setFcmToken(maxLengthFcmToken);
        request.setPlatform(DevicePlatform.ANDROID);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        FcmDto.Response response = memberService.upsertFcmProps(memberId, request);

        // then
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(member.getDeviceId()).isEqualTo(request.getDeviceId());
        assertThat(member.getFcmToken()).hasSize(512).isEqualTo(maxLengthFcmToken);
        assertThat(member.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 FCM 디바이스 정보는 저장할 수 없다")
    void upsertFcmPropsFailsWhenMemberDoesNotExist() {
        // given
        Long memberId = 1L;
        FcmDto.Request request = new FcmDto.Request();
        request.setDeviceId("550e8400-e29b-41d4-a716-446655440000");
        request.setFcmToken("fcm-token");
        request.setPlatform(DevicePlatform.IOS);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.upsertFcmProps(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 계정 권한(Role) 조회 성공 테스트")
    void findMyAccountRoleSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).role(MemberRole.ADMIN).build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        MyAccountDto.Response response = memberService.findMyAccountRole(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(MemberRole.ADMIN);
    }
}
