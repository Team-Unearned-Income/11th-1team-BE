package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.util.dto.MatchDetailDto;
import org.example.knockin.util.dto.MatchDto;
import org.example.knockin.util.dto.MatchListDto;
import org.example.knockin.declaration.dto.MemberReportDto;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.mate.service.impl.RoommateMatchingServiceImpl;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.declaration.entity.MemberDeclaration;
import org.example.knockin.member.entity.MemberInterest;
import org.example.knockin.member.service.impl.MemberInterestServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.example.knockin.declaration.repository.MemberDeclarationRepository;
import org.example.knockin.member.repository.MemberInterestRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.row.MatchingBasicInfoRow;
import org.example.knockin.room.repository.RoomOfferProfileRepository;
import org.example.knockin.room.repository.RoomSeekerProfileRepository;
import org.example.knockin.room.repository.row.MatchingOfferProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerRegionRow;
import org.example.knockin.room.repository.row.MatchingSeekerRoomTypeRow;
import org.example.knockin.room.service.impl.RoomOfferProfileServiceImpl;
import org.example.knockin.room.service.impl.RoomSeekerProfileServiceImpl;
import org.example.knockin.util.service.RoommateScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 매칭 서비스")
class RoommateMatchingServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberInterestRepository memberInterestRepository;

    @Mock
    private MemberDeclarationRepository memberDeclarationRepository;

    @Mock
    private RoomSeekerProfileRepository roomSeekerProfileRepository;

    @Mock
    private RoomOfferProfileRepository roomOfferProfileRepository;

    @Mock
    private MemberLifePatternRepository memberLifePatternRepository;

    @Mock
    private PreferenceConditionRepository preferenceConditionRepository;

    @Mock
    private PreferenceConditionWeightRepository preferenceConditionWeightRepository;

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private RoommateScoreService roommateScoreService;

    @InjectMocks
    private RoommateMatchingServiceImpl roommateMatchingService;

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
                null
        );
        MemberLifePatternService memberLifePatternService = new MemberLifePatternService(memberLifePatternRepository, null, null);
        PreferenceConditionServiceImpl preferenceConditionService = new PreferenceConditionServiceImpl(
                preferenceConditionRepository,
                null,
                null,
                preferenceConditionWeightRepository,
                null,
                null
        );
        AuthenticationServiceImpl authenticationService = new AuthenticationServiceImpl(
                authenticationRepository,
                null,
                memberService,
                null,
                null,
                null,
                null
        );

        roommateMatchingService = new RoommateMatchingServiceImpl(
                memberService,
                new MemberInterestServiceImpl(memberInterestRepository),
                new RoomSeekerProfileServiceImpl(roomSeekerProfileRepository),
                new RoomOfferProfileServiceImpl(roomOfferProfileRepository),
                memberLifePatternService,
                preferenceConditionService,
                authenticationService,
                new DeclarationServiceImpl(memberDeclarationRepository, null, null),
                roommateScoreService
        );
    }

    @Test
    @DisplayName("관심 이력이 없으면 매칭 대상 회원을 관심 목록에 활성 상태로 저장한다")
    void likeMatchingCreatesActiveInterestWhenNotExists() {
        // Given
        Long senderId = 1L;
        Long receiverId = 2L;
        Member sender = Member.builder().id(senderId).isDelete(false).build();
        Member receiver = Member.builder().id(receiverId).isDelete(false).build();

        when(memberRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(memberInterestRepository.findBySenderIdAndReceiverIdForUpdate(senderId, receiverId))
                .thenReturn(Optional.empty());

        // When
        MatchDto.Response response = roommateMatchingService.likeMatching(senderId, receiverId);

        // Then
        ArgumentCaptor<MemberInterest> memberInterestCaptor = ArgumentCaptor.forClass(MemberInterest.class);
        verify(memberInterestRepository).save(memberInterestCaptor.capture());
        MemberInterest memberInterest = memberInterestCaptor.getValue();
        assertThat(memberInterest.getSender()).isSameAs(sender);
        assertThat(memberInterest.getReceiver()).isSameAs(receiver);
        assertThat(memberInterest.getIsDeleted()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 관심 등록한 매칭 대상이면 기존 관심을 삭제 상태로 토글한다")
    void likeMatchingTogglesExistingActiveInterestToDeleted() {
        // Given
        Long senderId = 1L;
        Long receiverId = 2L;
        Member sender = Member.builder().id(senderId).isDelete(false).build();
        Member receiver = Member.builder().id(receiverId).isDelete(false).build();
        MemberInterest memberInterest = MemberInterest.builder()
                .sender(sender)
                .receiver(receiver)
                .isDeleted(false)
                .build();

        when(memberRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(memberInterestRepository.findBySenderIdAndReceiverIdForUpdate(senderId, receiverId))
                .thenReturn(Optional.of(memberInterest));

        // When
        MatchDto.Response response = roommateMatchingService.likeMatching(senderId, receiverId);

        // Then
        assertThat(memberInterest.getIsDeleted()).isTrue();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(memberInterestRepository, never()).save(any(MemberInterest.class));
    }

    @Test
    @DisplayName("삭제 상태의 매칭 관심이면 다시 활성 상태로 토글한다")
    void likeMatchingTogglesExistingDeletedInterestToActive() {
        // Given
        Long senderId = 1L;
        Long receiverId = 2L;
        Member sender = Member.builder().id(senderId).isDelete(false).build();
        Member receiver = Member.builder().id(receiverId).isDelete(false).build();
        MemberInterest memberInterest = MemberInterest.builder()
                .sender(sender)
                .receiver(receiver)
                .isDeleted(true)
                .build();

        when(memberRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(memberInterestRepository.findBySenderIdAndReceiverIdForUpdate(senderId, receiverId))
                .thenReturn(Optional.of(memberInterest));

        // When
        MatchDto.Response response = roommateMatchingService.likeMatching(senderId, receiverId);

        // Then
        assertThat(memberInterest.getIsDeleted()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(memberInterestRepository, never()).save(any(MemberInterest.class));
    }

    @Test
    @DisplayName("관심을 등록하는 회원이 없으면 회원 없음 예외를 던지고 대상 회원을 조회하지 않는다")
    void likeMatchingThrowsWhenSenderDoesNotExist() {
        // Given
        Long senderId = 1L;
        Long receiverId = 2L;
        when(memberRepository.findById(senderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateMatchingService.likeMatching(senderId, receiverId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verify(memberRepository, never()).findById(receiverId);
        verifyNoInteractions(memberInterestRepository);
    }

    @Test
    @DisplayName("관심 대상 회원이 없으면 회원 없음 예외를 던지고 관심 이력을 조회하지 않는다")
    void likeMatchingThrowsWhenReceiverDoesNotExist() {
        // Given
        Long senderId = 1L;
        Long receiverId = 2L;
        Member sender = Member.builder().id(senderId).isDelete(false).build();
        when(memberRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(receiverId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateMatchingService.likeMatching(senderId, receiverId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(memberInterestRepository);
    }

    @Test
    @DisplayName("신고 이력이 없으면 매칭 대상 회원 신고 사유를 저장한다")
    void reportMatchingSavesDeclarationWhenNotExists() {
        // Given
        Long reporterId = 1L;
        Long reportedId = 2L;
        Member reporter = Member.builder().id(reporterId).isDelete(false).build();
        Member reported = Member.builder().id(reportedId).isDelete(false).build();
        MemberReportDto.Request request = new MemberReportDto.Request();
        request.setContents("불쾌한 메시지를 반복해서 보냈습니다.");

        when(memberRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(memberRepository.findById(reportedId)).thenReturn(Optional.of(reported));
        when(memberDeclarationRepository.existsByReporterIdAndReportedId(reporter.getId(), reported.getId())).thenReturn(false);

        // When
        MemberReportDto.Response response = roommateMatchingService.reportMatching(reporterId, reportedId, request);

        // Then
        ArgumentCaptor<MemberDeclaration> declarationCaptor = ArgumentCaptor.forClass(MemberDeclaration.class);
        verify(memberDeclarationRepository).save(declarationCaptor.capture());
        MemberDeclaration declaration = declarationCaptor.getValue();
        assertThat(declaration.getReporterId()).isEqualTo(reporter.getId());
        assertThat(declaration.getReportedId()).isEqualTo(reported.getId());
        assertThat(declaration.getReason()).isEqualTo("불쾌한 메시지를 반복해서 보냈습니다.");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 신고한 매칭 대상이면 중복 신고 예외를 던지고 신고를 저장하지 않는다")
    void reportMatchingThrowsWhenDeclarationAlreadyExists() {
        // Given
        Long reporterId = 1L;
        Long reportedId = 2L;
        Member reporter = Member.builder().id(reporterId).isDelete(false).build();
        Member reported = Member.builder().id(reportedId).isDelete(false).build();
        MemberReportDto.Request request = new MemberReportDto.Request();
        request.setContents("이미 신고한 회원입니다.");

        when(memberRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(memberRepository.findById(reportedId)).thenReturn(Optional.of(reported));
        when(memberDeclarationRepository.existsByReporterIdAndReportedId(reporter.getId(), reported.getId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> roommateMatchingService.reportMatching(reporterId, reportedId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.DECLARATION_DUPLICATE));
        verify(memberDeclarationRepository, never()).save(any(MemberDeclaration.class));
    }

    @Test
    @DisplayName("신고하는 회원이 없으면 회원 없음 예외를 던지고 신고 대상과 신고 이력을 조회하지 않는다")
    void reportMatchingThrowsWhenReporterDoesNotExist() {
        // Given
        Long reporterId = 1L;
        Long reportedId = 2L;
        MemberReportDto.Request request = new MemberReportDto.Request();
        request.setContents("신고 사유입니다.");
        when(memberRepository.findById(reporterId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateMatchingService.reportMatching(reporterId, reportedId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verify(memberRepository, never()).findById(reportedId);
        verifyNoInteractions(memberDeclarationRepository);
    }

    @Test
    @DisplayName("신고 대상 회원이 없으면 회원 없음 예외를 던지고 신고 이력을 조회하지 않는다")
    void reportMatchingThrowsWhenReportedDoesNotExist() {
        // Given
        Long reporterId = 1L;
        Long reportedId = 2L;
        Member reporter = Member.builder().id(reporterId).isDelete(false).build();
        MemberReportDto.Request request = new MemberReportDto.Request();
        request.setContents("신고 사유입니다.");

        when(memberRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(memberRepository.findById(reportedId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateMatchingService.reportMatching(reporterId, reportedId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(memberDeclarationRepository);
    }

    @Test
    @DisplayName("조회 크기보다 후보가 많으면 요청 크기만 응답하고 다음 목록이 있음을 반환한다")
    void findMatchingListReturnsRequestedSizeAndHasNext() {
        // Given
        Long viewerId = 10L;
        MatchListDto.Request request = new MatchListDto.Request();
        request.setSize(2);
        request.setExcludeMemberIds(Arrays.asList(99L, null, 99L));

        LocalDate offerBirth = LocalDate.of(2000, 1, 1);
        LocalDate seekerBirth = LocalDate.of(1999, 5, 10);

        when(memberRepository.findMatchingBasicRow(List.of(99L, viewerId), 3, null, viewerId))
                .thenReturn(List.of(
                        new MatchingBasicInfoRow(1L, "offer-profile.png", "오퍼", offerBirth, Gender.MALE, 101L, RoomProfileType.OFFER),
                        new MatchingBasicInfoRow(2L, "seeker-profile.png", "시커", seekerBirth, Gender.FEMALE, 102L, RoomProfileType.SEEKER),
                        new MatchingBasicInfoRow(3L, "next-profile.png", "다음", LocalDate.of(2001, 3, 3), Gender.MALE, 103L, RoomProfileType.OFFER)
                ));
        when(roomOfferProfileRepository.findAllOfferProfileByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new MatchingOfferProfileRow(1L, 500, 45, "역삼동", "강남구", "서울특별시", "원룸")));
        when(roomSeekerProfileRepository.findAllSeekerProfileByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new MatchingSeekerProfileRow(2L, 300, 1000, 30, 70)));
        when(roomSeekerProfileRepository.findAllSeekerRegionByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new MatchingSeekerRegionRow(2L, "성수동", "성동구", "서울특별시")));
        when(roomSeekerProfileRepository.findAllSeekerRoomTypeByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new MatchingSeekerRoomTypeRow(2L, "투룸")));
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        new MatchingLifestyleRow(
                                1L, 11L, 101L, 1001L, "청결", "4", "깔끔함",
                                LifePatternType.SCALE, "cleaning.png"),
                        new MatchingLifestyleRow(
                                2L, 12L, 102L, 1002L, "흡연", "2", "비흡연자",
                                LifePatternType.SINGLE_CHOICE, "smoking.png")
                ));
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new MatchingPreferenceConditionRow(
                        2L, 21L, 103L, 1003L, "소음", "1", "조용함",
                        LifePatternType.SCALE, "noise.png")));
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new MatchingPreferenceConditionWeightRow(
                        2L, 31L, 102L, "흡연", "smoking.png")));
        when(authenticationRepository.findAcceptedByMemberIds(List.of(1L, 2L)))
                .thenReturn(List.of(
                        new MemberAuthenticationRow(1L, AuthenticationType.STUDENT),
                        new MemberAuthenticationRow(2L, AuthenticationType.COMPANY)
                ));
        when(memberInterestRepository.findActiveReceiverIdsBySenderIdAndReceiverIds(viewerId, List.of(1L, 2L)))
                .thenReturn(List.of(2L));
        when(roommateScoreService.calculateScores(any(), anyList()))
                .thenReturn(Map.of(
                        1L, new Compatibility(80, List.of()),
                        2L, new Compatibility(60, List.of())
                ));

        // When
        Slice<MatchListDto.Response> response = roommateMatchingService.findMatchingList(viewerId, request);

        // Then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.getContent()).hasSize(2);

        MatchListDto.Response offer = response.getContent().get(0);
        assertThat(offer.getMemberId()).isEqualTo(1L);
        assertThat(offer.getMemberName()).isEqualTo("오퍼");
        assertThat(offer.getMemberAge()).isEqualTo(DateUtils.calculateAge(offerBirth));
        assertThat(offer.getInterested()).isFalse();
        assertThat(offer.getScore()).isEqualTo(80);
        assertThat(offer.getOfferProfile().getDeposit()).isEqualTo(500);
        assertThat(offer.getOfferProfile().getMonthlyRent()).isEqualTo(45);
        assertThat(offer.getOfferProfile().getRegionFullName()).isEqualTo("서울특별시 강남구 역삼동");
        assertThat(offer.getOfferProfile().getRoomTypeName()).isEqualTo("원룸");
        assertThat(offer.getSeekerProfile()).isNull();
        assertThat(offer.getLifeStyles()).extracting(MatchListDto.Lifestyle::getImageUrl)
                .containsExactly("cleaning.png");
        assertThat(offer.getLifeStyles()).extracting(MatchListDto.Lifestyle::getName).containsExactly("청결");
        assertThat(offer.getAuthentications()).containsExactly(AuthenticationType.STUDENT);

        MatchListDto.Response seeker = response.getContent().get(1);
        assertThat(seeker.getMemberId()).isEqualTo(2L);
        assertThat(seeker.getMemberName()).isEqualTo("시커");
        assertThat(seeker.getMemberAge()).isEqualTo(DateUtils.calculateAge(seekerBirth));
        assertThat(seeker.getInterested()).isTrue();
        assertThat(seeker.getOfferProfile()).isNull();
        assertThat(seeker.getSeekerProfile().getMinDeposit()).isEqualTo(300);
        assertThat(seeker.getSeekerProfile().getMaxDeposit()).isEqualTo(1000);
        assertThat(seeker.getSeekerProfile().getRoomTypeNames()).containsExactly("투룸");
        assertThat(seeker.getSeekerProfile().getRegionFullNames()).containsExactly("서울특별시 성동구 성수동");
        assertThat(seeker.getConditions()).extracting(MatchListDto.Condition::getName).containsExactly("소음");
        assertThat(seeker.getConditionWeights()).extracting(MatchListDto.ConditionWeight::getName).containsExactly("흡연");
        assertThat(seeker.getAuthentications()).containsExactly(AuthenticationType.COMPANY);

        verify(memberRepository).findMatchingBasicRow(List.of(99L, viewerId), 3, null, viewerId);
        verify(roomOfferProfileRepository).findAllOfferProfileByMemberIdIn(List.of(1L, 2L));
        verify(roomSeekerProfileRepository).findAllSeekerProfileByMemberIdIn(List.of(1L, 2L));
        verify(authenticationRepository).findAcceptedByMemberIds(List.of(1L, 2L));
    }

    @Test
    @DisplayName("후보 회원이 없으면 추가 조회 없이 빈 Slice를 반환한다")
    void findMatchingListReturnsEmptySliceWithoutLoadingDetails() {
        // Given
        Long viewerId = 10L;
        MatchListDto.Request request = new MatchListDto.Request();
        request.setSize(20);
        when(memberRepository.findMatchingBasicRow(List.of(viewerId), 21, null, viewerId))
                .thenReturn(List.of());

        // When
        Slice<MatchListDto.Response> response = roommateMatchingService.findMatchingList(viewerId, request);

        // Then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verify(memberRepository).findMatchingBasicRow(List.of(viewerId), 21, null, viewerId);
        verifyNoInteractions(
                roomOfferProfileRepository,
                roomSeekerProfileRepository,
                memberLifePatternRepository,
                preferenceConditionRepository,
                preferenceConditionWeightRepository,
                memberInterestRepository
        );
    }

    @Test
    @DisplayName("비로그인 사용자는 관심 조회를 하지 않고 좋아요 여부를 false로 반환한다")
    void findMatchingListDoesNotLoadLikesForAnonymousUser() {
        // Given
        MatchListDto.Request request = new MatchListDto.Request();
        request.setSize(1);

        when(memberRepository.findMatchingBasicRow(List.of(), 2, null, null))
                .thenReturn(List.of(new MatchingBasicInfoRow(
                        1L,
                        "offer-profile.png",
                        "오퍼",
                        LocalDate.of(2000, 1, 1),
                        Gender.MALE,
                        101L,
                        RoomProfileType.OFFER
                )));
        when(roomOfferProfileRepository.findAllOfferProfileByMemberIdIn(List.of(1L)))
                .thenReturn(List.of(new MatchingOfferProfileRow(1L, 500, 45, "역삼동", "강남구", "서울특별시", "원룸")));
        when(roomSeekerProfileRepository.findAllSeekerProfileByMemberIdIn(List.of(1L))).thenReturn(List.of());
        when(roomSeekerProfileRepository.findAllSeekerRegionByMemberIdIn(List.of(1L))).thenReturn(List.of());
        when(roomSeekerProfileRepository.findAllSeekerRoomTypeByMemberIdIn(List.of(1L))).thenReturn(List.of());
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(1L))).thenReturn(List.of());
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(1L))).thenReturn(List.of());
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(List.of(1L))).thenReturn(List.of());

        // When
        Slice<MatchListDto.Response> response = roommateMatchingService.findMatchingList(null, request);

        // Then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getInterested()).isFalse();
        assertThat(response.hasNext()).isFalse();
        verify(memberInterestRepository, never()).findActiveReceiverIdsBySenderIdAndReceiverIds(any(), anyList());
    }

    @Test
    @DisplayName("관심 회원 필터는 로그인 회원 ID를 후보 조회 조건으로 전달한다")
    void findMatchingListPassesRequesterIdForLikedOnlyFilter() {
        Long requesterId = 10L;
        MatchListDto.Request request = new MatchListDto.Request();
        request.setLikedOnly(true);
        request.setSize(20);
        when(memberRepository.findMatchingBasicRow(
                List.of(requesterId), 21, requesterId, requesterId))
                .thenReturn(List.of());

        Slice<MatchListDto.Response> response =
                roommateMatchingService.findMatchingList(requesterId, request);

        assertThat(response).isEmpty();
        verify(memberRepository).findMatchingBasicRow(
                List.of(requesterId), 21, requesterId, requesterId);
    }

    @Test
    @DisplayName("비로그인 사용자의 관심 회원 필터 요청은 인증 예외를 던진다")
    void findMatchingListRejectsAnonymousLikedOnlyRequest() {
        MatchListDto.Request request = new MatchListDto.Request();
        request.setLikedOnly(true);

        assertThatThrownBy(() -> roommateMatchingService.findMatchingList(null, request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED)
                );
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("방이 있는 회원의 상세 조회는 방 있음 프로필과 인증 정보를 반환한다")
    void findMatchingDetailReturnsOfferProfileAndAuthentications() {
        // Given
        Long targetMemberId = 1L;
        Long requesterId = 10L;
        LocalDate birth = LocalDate.of(2000, 1, 1);

        when(memberRepository.findMatchingBasicRowById(targetMemberId))
                .thenReturn(Optional.of(new MatchingBasicInfoRow(
                        targetMemberId,
                        "offer-profile.png",
                        "오퍼",
                        birth,
                        Gender.MALE,
                        101L,
                        RoomProfileType.OFFER
                )));
        when(authenticationRepository.getAcceptedAuthenticationTypeByMemberId(targetMemberId))
                .thenReturn(List.of(AuthenticationType.STUDENT));
        when(roomOfferProfileRepository.findAllOfferProfileByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingOfferProfileRow(targetMemberId, 500, 45, "역삼동", "강남구", "서울특별시", "원룸")));
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingLifestyleRow(
                        targetMemberId, 11L, 101L, 1001L, "청결", "4", "깔끔함",
                        LifePatternType.SCALE, "cleaning.png")));
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingPreferenceConditionRow(
                        targetMemberId, 21L, 103L, 1003L, "소음", "1", "조용함",
                        LifePatternType.SCALE, "noise.png")));
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingPreferenceConditionWeightRow(
                        targetMemberId, 31L, 102L, "흡연", "smoking.png")));
        when(memberInterestRepository.existsBySenderIdAndReceiverIdAndIsDeletedIsFalse(
                requesterId, targetMemberId)).thenReturn(true);
        when(roommateScoreService.calculateScore(any(), any()))
                .thenReturn(new Compatibility(87, List.of()));

        // When
        MatchDetailDto.Response response = roommateMatchingService.findMatchingDetail(targetMemberId, requesterId);

        // Then
        assertThat(response.getMemberId()).isEqualTo(targetMemberId);
        assertThat(response.getMemberProfileImageUrl()).isEqualTo("offer-profile.png");
        assertThat(response.getMemberName()).isEqualTo("오퍼");
        assertThat(response.getMemberAge()).isEqualTo(DateUtils.calculateAge(birth));
        assertThat(response.getGender()).isEqualTo(Gender.MALE);
        assertThat(response.getInterested()).isTrue();
        assertThat(response.getRoomProfileType()).isEqualTo(RoomProfileType.OFFER);
        assertThat(response.getOfferProfile().getDeposit()).isEqualTo(500);
        assertThat(response.getOfferProfile().getMonthlyRent()).isEqualTo(45);
        assertThat(response.getOfferProfile().getRegionFullName()).isEqualTo("서울특별시 강남구 역삼동");
        assertThat(response.getOfferProfile().getRoomTypeName()).isEqualTo("원룸");
        assertThat(response.getSeekerProfile()).isNull();
        assertThat(response.getLifeStyles()).extracting(MatchListDto.Lifestyle::getName).containsExactly("청결");
        assertThat(response.getConditions()).extracting(MatchListDto.Condition::getName).containsExactly("소음");
        assertThat(response.getConditionWeights()).extracting(MatchListDto.ConditionWeight::getName).containsExactly("흡연");
        assertThat(response.getAuthentications()).containsExactly(AuthenticationType.STUDENT);
        assertThat(response.getCompatibility().getTotalScore()).isEqualTo(87);
        assertThat(response.isMine()).isFalse();
        verifyNoInteractions(roomSeekerProfileRepository);
    }

    @Test
    @DisplayName("방이 없는 회원의 상세 조회는 방 없음 프로필만 반환하고 비로그인은 관심 조회를 하지 않는다")
    void findMatchingDetailReturnsSeekerProfileForAnonymousUser() {
        // Given
        Long targetMemberId = 2L;
        LocalDate birth = LocalDate.of(1999, 5, 10);

        when(memberRepository.findMatchingBasicRowById(targetMemberId))
                .thenReturn(Optional.of(new MatchingBasicInfoRow(
                        targetMemberId,
                        "seeker-profile.png",
                        "시커",
                        birth,
                        Gender.FEMALE,
                        102L,
                        RoomProfileType.SEEKER
                )));
        when(authenticationRepository.getAcceptedAuthenticationTypeByMemberId(targetMemberId)).thenReturn(List.of());
        when(roomSeekerProfileRepository.findAllSeekerProfileByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingSeekerProfileRow(targetMemberId, 300, 1000, 30, 70)));
        when(roomSeekerProfileRepository.findAllSeekerRegionByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingSeekerRegionRow(targetMemberId, "성수동", "성동구", "서울특별시")));
        when(roomSeekerProfileRepository.findAllSeekerRoomTypeByMemberIdIn(List.of(targetMemberId)))
                .thenReturn(List.of(new MatchingSeekerRoomTypeRow(targetMemberId, "투룸")));
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(targetMemberId))).thenReturn(List.of());
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(targetMemberId))).thenReturn(List.of());
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(List.of(targetMemberId))).thenReturn(List.of());

        // When
        MatchDetailDto.Response response = roommateMatchingService.findMatchingDetail(targetMemberId, null);

        // Then
        assertThat(response.getMemberId()).isEqualTo(targetMemberId);
        assertThat(response.getMemberName()).isEqualTo("시커");
        assertThat(response.getMemberAge()).isEqualTo(DateUtils.calculateAge(birth));
        assertThat(response.getInterested()).isFalse();
        assertThat(response.getRoomProfileType()).isEqualTo(RoomProfileType.SEEKER);
        assertThat(response.getOfferProfile()).isNull();
        assertThat(response.getSeekerProfile().getMinDeposit()).isEqualTo(300);
        assertThat(response.getSeekerProfile().getMaxDeposit()).isEqualTo(1000);
        assertThat(response.getSeekerProfile().getMinMonthlyRent()).isEqualTo(30);
        assertThat(response.getSeekerProfile().getMaxMonthlyRent()).isEqualTo(70);
        assertThat(response.getSeekerProfile().getRoomTypeNames()).containsExactly("투룸");
        assertThat(response.getSeekerProfile().getRegionFullNames()).containsExactly("서울특별시 성동구 성수동");
        assertThat(response.getAuthentications()).isEmpty();
        assertThat(response.isMine()).isFalse();
        verifyNoInteractions(roomOfferProfileRepository);
        verify(memberInterestRepository, never())
                .existsBySenderIdAndReceiverIdAndIsDeletedIsFalse(any(), any());
    }

    @Test
    @DisplayName("상세 조회는 본인 프로필을 조회하면 본인 여부를 true로 응답한다")
    void findMatchingDetailReturnsMineTrueForTargetMember() {
        // Given
        Long memberId = 1L;
        when(memberRepository.findMatchingBasicRowById(memberId))
                .thenReturn(Optional.of(new MatchingBasicInfoRow(
                        memberId,
                        "profile.png",
                        "본인",
                        LocalDate.of(2000, 1, 1),
                        Gender.MALE,
                        101L,
                        RoomProfileType.OFFER
                )));
        when(authenticationRepository.getAcceptedAuthenticationTypeByMemberId(memberId))
                .thenReturn(List.of());
        when(roomOfferProfileRepository.findAllOfferProfileByMemberIdIn(List.of(memberId)))
                .thenReturn(List.of());
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(memberId)))
                .thenReturn(List.of());
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(memberId)))
                .thenReturn(List.of());
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(List.of(memberId)))
                .thenReturn(List.of());

        // When
        MatchDetailDto.Response response = roommateMatchingService.findMatchingDetail(memberId, memberId);

        // Then
        assertThat(response.isMine()).isTrue();
    }

    @Test
    @DisplayName("상세 조회 대상 회원이 없으면 회원 없음 예외를 던지고 추가 정보를 조회하지 않는다")
    void findMatchingDetailThrowsWhenTargetMemberDoesNotExist() {
        // Given
        Long targetMemberId = 999L;
        when(memberRepository.findMatchingBasicRowById(targetMemberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateMatchingService.findMatchingDetail(targetMemberId, 10L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(
                authenticationRepository,
                roomOfferProfileRepository,
                roomSeekerProfileRepository,
                memberLifePatternRepository,
                preferenceConditionRepository,
                preferenceConditionWeightRepository,
                memberInterestRepository
        );
    }
}
