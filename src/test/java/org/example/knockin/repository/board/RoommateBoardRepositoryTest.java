package org.example.knockin.repository.board;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.example.knockin.board.dto.BoardDetailDto;
import org.example.knockin.board.repository.RoommateBoardFileRepository;
import org.example.knockin.board.repository.RoommateBoardInterestRepository;
import org.example.knockin.board.repository.RoommateBoardOptionRepository;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.board.dto.BoardDetailDto.Response.RoomExtraOptionInfo;
import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.example.knockin.board.entity.RoommateBoardOption;
import org.example.knockin.member.entity.BasicInformationFile;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternFile;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePattern;
import org.example.knockin.life.entity.PreferenceCondition;
import org.example.knockin.life.entity.PreferenceConditionWeight;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Block;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.member.entity.State;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.entity.RoomExtraOptionFile;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.entity.RoomTypeFile;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.board.repository.row.BasicInfoRow;
import org.example.knockin.board.repository.row.BoardBaseRow;
import org.example.knockin.board.repository.row.BoardInterestCountRow;
import org.example.knockin.board.repository.row.BoardThumbnailRow;
import org.example.knockin.board.repository.row.EditFormRow;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("룸메이트 게시글 Repository")
class RoommateBoardRepositoryTest {

    @Autowired
    private RoommateBoardRepository roommateBoardRepository;

    @Autowired
    private RoommateBoardFileRepository roommateBoardFileRepository;

    @Autowired
    private RoommateBoardOptionRepository roommateBoardOptionRepository;

    @Autowired
    private RoommateBoardInterestRepository roommateBoardInterestRepository;

    @Autowired
    private MemberLifePatternRepository memberLifePatternRepository;

    @Autowired
    private PreferenceConditionRepository preferenceConditionRepository;

    @Autowired
    private PreferenceConditionWeightRepository preferenceConditionWeightRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("입주 가능시기가 노출 기준일 이전인 게시글은 목록에서 제외한다")
    void searchExcludesBoardsBeforeVisibleEndDate() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-visible");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("서초동", 3, null);
        persistBoard("기준일 게시글", member, roomType, region, visibleEndDate);
        persistBoard("미래 게시글", member, roomType, region, visibleEndDate.plusDays(1));
        persistBoard("숨김 게시글", member, roomType, region, visibleEndDate.minusSeconds(1));
        entityManager.flush();
        entityManager.clear();

        BoardListDto.Request request = defaultRequest();
        PageRequest pageable = PageRequest.of(0, 20);

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(request, pageable, visibleEndDate, null);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(BoardBaseRow::title)
                .containsExactlyInAnyOrder("기준일 게시글", "미래 게시글")
                .doesNotContain("숨김 게시글");
    }

    @Test
    @DisplayName("입주 시기 협의 가능 게시글은 입주 가능일이 없어도 목록에 포함한다")
    void searchIncludesNegotiableBoardWithoutComeableDate() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-negotiable");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("서초동", 3, null);
        persistBoard("협의 가능 게시글", member, roomType, region, null, true);
        persistBoard("입주일 없음 게시글", member, roomType, region, null, false);
        persistBoard("지난 입주일 게시글", member, roomType, region, visibleEndDate.minusSeconds(1), false);
        entityManager.flush();
        entityManager.clear();

        BoardListDto.Request request = defaultRequest();
        PageRequest pageable = PageRequest.of(0, 20);

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(request, pageable, visibleEndDate, null);

        // Then
        assertThat(result.getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("협의 가능 게시글");
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("조회 페이지에 게시글이 없으면 다음 페이지가 없는 빈 Slice를 반환한다")
    void searchReturnsEmptySliceWhenRequestedPageIsEmpty() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-empty-slice");
        RoomType roomType = persistRoomType("투룸");
        Region region = persistRegion("역삼동", 3, null);
        persistBoard("첫 번째 게시글", member, roomType, region, visibleEndDate.plusDays(1));
        persistBoard("두 번째 게시글", member, roomType, region, visibleEndDate.plusDays(2));
        entityManager.flush();
        entityManager.clear();

        BoardListDto.Request request = defaultRequest();
        PageRequest pageable = PageRequest.of(1, 20);

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(request, pageable, visibleEndDate, null);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.isLast()).isTrue();
        assertThat(result.getPageable()).isEqualTo(pageable);
    }

    @ParameterizedTest
    @CsvSource({"0, 0, false", "19, 19, false", "20, 20, false", "21, 20, true"})
    @DisplayName("목록은 페이지 크기까지만 반환하고 추가 게시글이 있을 때만 다음 페이지가 있다")
    void searchDeterminesHasNextAtPageSizeBoundary(int boardCount, int expectedSize, boolean expectedHasNext) {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-slice-boundary");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("역삼동", 3, null);
        for (int i = 0; i < boardCount; i++) {
            persistBoard("게시글 " + i, member, roomType, region, visibleEndDate.plusDays(1));
        }
        entityManager.flush();
        entityManager.clear();
        PageRequest pageable = PageRequest.of(0, 20);

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(
                defaultRequest(), pageable, visibleEndDate, null);

        // Then
        assertThat(result.getContent()).hasSize(expectedSize);
        assertThat(result.hasNext()).isEqualTo(expectedHasNext);
        assertThat(result.isLast()).isEqualTo(!expectedHasNext);
        assertThat(result.getPageable()).isEqualTo(pageable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"createdAt", "hits"})
    @DisplayName("정렬 값이 같은 게시글도 ID 내림차순으로 이어 조회하고 추가 조회한 행을 누락하지 않는다")
    void searchReturnsConsecutiveSlicesWithoutLosingLookaheadRow(String sortProperty) {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-consecutive-slices");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("역삼동", 3, null);
        List<Long> boardIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            boardIds.add(persistBoard("게시글 " + i, member, roomType, region, visibleEndDate.plusDays(1)).getId());
        }
        entityManager.flush();
        entityManager.createNativeQuery("update roommate_board set created_at = :createdAt")
                .setParameter("createdAt", visibleEndDate)
                .executeUpdate();
        entityManager.clear();
        PageRequest firstPage = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, sortProperty));
        BoardListDto.Request request = defaultRequest();

        // When
        Slice<BoardBaseRow> first = roommateBoardRepository.search(request, firstPage, visibleEndDate, null);
        Slice<BoardBaseRow> second = roommateBoardRepository.search(request, firstPage.next(), visibleEndDate, null);
        Slice<BoardBaseRow> third = roommateBoardRepository.search(request, firstPage.next().next(), visibleEndDate, null);

        // Then
        assertThat(first.getContent()).extracting(BoardBaseRow::boardId)
                .containsExactly(boardIds.get(4), boardIds.get(3));
        assertThat(second.getContent()).extracting(BoardBaseRow::boardId)
                .containsExactly(boardIds.get(2), boardIds.get(1));
        assertThat(third.getContent()).extracting(BoardBaseRow::boardId)
                .containsExactly(boardIds.get(0));
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isTrue();
        assertThat(third.hasNext()).isFalse();
    }

    @Test
    @DisplayName("성별 필터는 회원의 최신 기본정보를 기준으로 게시글을 조회한다")
    void searchFiltersGenderByLatestBasicInformation() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-gender");
        BasicInformation oldBasicInformation = persistBasicInformation(
                member, "이전정보", Gender.MALE, "old@example.com", LocalDate.of(2000, 1, 1));
        persistBasicInformationFile(oldBasicInformation, persistFile("old-profile.jpg"));
        LocalDate latestBirth = LocalDate.of(1997, 5, 20);
        BasicInformation latestBasicInformation = persistBasicInformation(
                member, "최신정보", Gender.FEMALE, "new@example.com", latestBirth);
        persistBasicInformationFile(latestBasicInformation, persistFile("profile-old.jpg"));
        persistBasicInformationFile(latestBasicInformation, persistFile("profile-latest.jpg"));
        RoomType roomType = persistRoomType("오피스텔");
        Region region = persistRegion("합정동", 3, null);
        persistBoard("최신 성별 기준 게시글", member, roomType, region, visibleEndDate.plusDays(1));
        entityManager.flush();
        entityManager.clear();

        BoardListDto.Request femaleRequest = request(
                null,
                null,
                Gender.FEMALE,
                null,
                null,
                null,
                null
        );
        BoardListDto.Request maleRequest = request(
                null,
                null,
                Gender.MALE,
                null,
                null,
                null,
                null
        );
        PageRequest pageable = PageRequest.of(0, 20);

        // When
        Slice<BoardBaseRow> femaleResult = roommateBoardRepository.search(femaleRequest, pageable, visibleEndDate, null);
        Slice<BoardBaseRow> maleResult = roommateBoardRepository.search(maleRequest, pageable, visibleEndDate, null);

        // Then
        assertThat(femaleResult.getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("최신 성별 기준 게시글");
        assertThat(femaleResult.getContent()).singleElement().satisfies(row -> {
            assertThat(row.memberName()).isEqualTo("최신정보");
            assertThat(row.memberProfileImageUrl()).isEqualTo("profile-latest.jpg");
            assertThat(row.memberBirth()).isEqualTo(latestBirth);
            assertThat(row.memberGender()).isEqualTo(Gender.FEMALE);
        });
        assertThat(maleResult.getContent()).isEmpty();
    }

    @Test
    @DisplayName("키워드는 게시글 제목, 지역 계층, 방 유형을 기준으로 검색한다")
    void searchFiltersBoardsByKeyword() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member member = persistMember("provider-keyword");
        Region seoul = persistRegion("서울", 1, null);
        Region gangnam = persistRegion("강남구", 2, seoul);
        Region yeoksam = persistRegion("역삼동", 3, gangnam);
        Region busan = persistRegion("부산", 1, null);
        Region haeundae = persistRegion("해운대구", 2, busan);
        Region jungdong = persistRegion("중동", 3, haeundae);
        RoomType oneRoom = persistRoomType("원룸");
        RoomType officetel = persistRoomType("오피스텔");
        persistBoard("햇살 좋은 집", member, oneRoom, yeoksam, visibleEndDate.plusDays(1));
        persistBoard("조용한 방", member, officetel, jungdong, visibleEndDate.plusDays(1));
        entityManager.flush();
        entityManager.clear();

        BoardListDto.Request request = defaultRequest();
        PageRequest pageable = PageRequest.of(0, 20);

        // When & Then
        request.setKeyword("햇살");
        assertThat(roommateBoardRepository.search(request, pageable, visibleEndDate, null).getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("햇살 좋은 집");

        request.setKeyword("강남구");
        assertThat(roommateBoardRepository.search(request, pageable, visibleEndDate, null).getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("햇살 좋은 집");

        request.setKeyword("오피스");
        assertThat(roommateBoardRepository.search(request, pageable, visibleEndDate, null).getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("조용한 방");

        request.setKeyword("  해운대  ");
        assertThat(roommateBoardRepository.search(request, pageable, visibleEndDate, null).getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("조용한 방");

        request.setKeyword("   ");
        assertThat(roommateBoardRepository.search(request, pageable, visibleEndDate, null).getContent())
                .extracting(BoardBaseRow::title)
                .containsExactlyInAnyOrder("햇살 좋은 집", "조용한 방");
    }

    @Test
    @DisplayName("관심 게시글 필터는 요청 회원이 활성화한 게시글만 조회한다")
    void searchFiltersOnlyActiveInterestedBoards() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member requester = persistMember("provider-liked-requester");
        Member owner = persistMember("provider-liked-owner");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("역삼동", 3, null);
        RoommateBoard activeBoard = persistBoard(
                "활성 관심 게시글", owner, roomType, region, visibleEndDate.plusDays(1));
        RoommateBoard deletedBoard = persistBoard(
                "해제한 관심 게시글", owner, roomType, region, visibleEndDate.plusDays(2));
        persistBoard("관심하지 않은 게시글", owner, roomType, region, visibleEndDate.plusDays(3));
        entityManager.persist(RoommateBoardInterest.builder()
                .member(requester)
                .roommateBoard(activeBoard)
                .isDeleted(false)
                .build());
        entityManager.persist(RoommateBoardInterest.builder()
                .member(requester)
                .roommateBoard(deletedBoard)
                .isDeleted(true)
                .build());
        entityManager.flush();
        entityManager.clear();

        BoardListDto.Request request = defaultRequest();
        request.setLikedOnly(true);

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(
                request,
                PageRequest.of(0, 20),
                visibleEndDate,
                requester.getId()
        );

        // Then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("활성 관심 게시글");
        assertThat(roommateBoardInterestRepository.findActiveBoardIdsByMemberIdAndBoardIds(
                requester.getId(),
                List.of(activeBoard.getId(), deletedBoard.getId())
        )).containsExactly(activeBoard.getId());
    }

    @Test
    @DisplayName("게시글별 관심 수는 활성 관심만 집계한다")
    void findActiveInterestCountsByBoardIdsCountsOnlyActiveInterests() {
        // Given
        Member owner = persistMember("provider-interest-owner");
        Member activeMember = persistMember("provider-interest-active");
        Member deletedMember = persistMember("provider-interest-deleted");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("역삼동", 3, null);
        RoommateBoard interestedBoard = persistBoard(
                "관심 집계 게시글", owner, roomType, region, LocalDateTime.now().plusDays(1));
        RoommateBoard noInterestBoard = persistBoard(
                "관심 없는 게시글", owner, roomType, region, LocalDateTime.now().plusDays(1));
        entityManager.persist(RoommateBoardInterest.builder()
                .member(activeMember)
                .roommateBoard(interestedBoard)
                .isDeleted(false)
                .build());
        entityManager.persist(RoommateBoardInterest.builder()
                .member(deletedMember)
                .roommateBoard(interestedBoard)
                .isDeleted(true)
                .build());
        entityManager.flush();
        entityManager.clear();

        // When
        List<BoardInterestCountRow> result =
                roommateBoardInterestRepository.findActiveInterestCountsByBoardIds(
                        List.of(interestedBoard.getId(), noInterestBoard.getId()));

        // Then
        assertThat(result)
                .containsExactly(new BoardInterestCountRow(interestedBoard.getId(), 1L));
    }

    @Test
    @DisplayName("게시글 목록은 요청자와 양방향 활성 차단 관계인 작성자의 게시글을 제외한다")
    void searchExcludesBoardsWrittenByBlockedMembersInBothDirections() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member requester = persistMember("provider-block-requester");
        Member blockedByRequester = persistMember("provider-blocked-by-requester");
        Member requesterBlockedBy = persistMember("provider-requester-blocked-by");
        Member deletedOutgoingBlockOwner = persistMember("provider-deleted-outgoing-block-owner");
        Member deletedIncomingBlockOwner = persistMember("provider-deleted-incoming-block-owner");
        Member visibleOwner = persistMember("provider-visible-owner");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("역삼동", 3, null);
        persistBoard("내가 차단한 작성자 게시글", blockedByRequester, roomType, region, visibleEndDate.plusDays(1));
        persistBoard("나를 차단한 작성자 게시글", requesterBlockedBy, roomType, region, visibleEndDate.plusDays(2));
        persistBoard("내가 차단 해제한 작성자 게시글", deletedOutgoingBlockOwner, roomType, region, visibleEndDate.plusDays(3));
        persistBoard("나를 차단 해제한 작성자 게시글", deletedIncomingBlockOwner, roomType, region, visibleEndDate.plusDays(3));
        persistBoard("노출 작성자 게시글", visibleOwner, roomType, region, visibleEndDate.plusDays(4));
        persistBlock(requester, blockedByRequester, false);
        persistBlock(requesterBlockedBy, requester, false);
        persistBlock(blockedByRequester, requester, true);
        persistBlock(requester, requesterBlockedBy, true);
        persistBlock(requester, deletedOutgoingBlockOwner, true);
        persistBlock(deletedIncomingBlockOwner, requester, true);
        persistBlock(visibleOwner, blockedByRequester, false);
        persistBlock(requesterBlockedBy, visibleOwner, false);
        entityManager.flush();
        entityManager.clear();

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(
                defaultRequest(),
                PageRequest.of(0, 3),
                visibleEndDate,
                requester.getId()
        );

        // Then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(BoardBaseRow::title)
                .containsExactlyInAnyOrder("내가 차단 해제한 작성자 게시글", "나를 차단 해제한 작성자 게시글", "노출 작성자 게시글")
                .doesNotContain("내가 차단한 작성자 게시글", "나를 차단한 작성자 게시글");
    }

    @Test
    @DisplayName("비로그인 목록 조회는 회원 사이에 활성 차단이 있어도 게시글을 제외하지 않는다")
    void searchDoesNotFilterBlockedMembersForAnonymousRequester() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member firstMember = persistMember("provider-anonymous-first");
        Member secondMember = persistMember("provider-anonymous-second");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("역삼동", 3, null);
        persistBoard("첫 회원 게시글", firstMember, roomType, region, visibleEndDate.plusDays(1));
        persistBoard("두 번째 회원 게시글", secondMember, roomType, region, visibleEndDate.plusDays(1));
        persistBlock(firstMember, secondMember, false);
        persistBlock(secondMember, firstMember, false);
        entityManager.flush();
        entityManager.clear();

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(
                defaultRequest(), PageRequest.of(0, 20), visibleEndDate, null);

        // Then
        assertThat(result.getContent()).extracting(BoardBaseRow::title)
                .containsExactlyInAnyOrder("첫 회원 게시글", "두 번째 회원 게시글");
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("게시글 목록은 비활성 회원이 작성한 게시글을 제외한다")
    void searchExcludesBoardsWrittenByInactiveMembers() {
        // Given
        LocalDateTime visibleEndDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member activeOwner = persistMember("provider-active-owner");
        Member inactiveOwner = persistMember("provider-inactive-owner", MemberState.INACTIVE);
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("서초동", 3, null);
        persistBoard("활성 회원 게시글", activeOwner, roomType, region, visibleEndDate.plusDays(1));
        persistBoard("비활성 회원 게시글", inactiveOwner, roomType, region, visibleEndDate.plusDays(2));
        entityManager.flush();
        entityManager.clear();

        // When
        Slice<BoardBaseRow> result = roommateBoardRepository.search(
                defaultRequest(),
                PageRequest.of(0, 20),
                visibleEndDate,
                null
        );

        // Then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(BoardBaseRow::title)
                .containsExactly("활성 회원 게시글")
                .doesNotContain("비활성 회원 게시글");
    }

    @Test
    @DisplayName("상세 기본 정보 조회는 응답 가공 전 원천 데이터를 반환한다")
    void getBasicInfoReturnsRawBasicInfoRow() {
        // Given
        Member member = persistMember("provider-detail");
        LocalDate birth = LocalDate.of(1998, 1, 1);
        BasicInformation oldBasicInformation = persistBasicInformation(member, "이전작성자", Gender.MALE, "old-detail@example.com", LocalDate.of(2000, 1, 1));
        persistBasicInformationFile(oldBasicInformation, persistFile("old-profile.png"));
        BasicInformation latestBasicInformation = persistBasicInformation(member, "상세작성자", Gender.FEMALE, "detail@example.com", birth);
        persistBasicInformationFile(latestBasicInformation, persistFile("profile-old.png"));
        persistBasicInformationFile(latestBasicInformation, persistFile("profile-latest.png"));

        Region city = persistRegion("서울", 1, null);
        Region district = persistRegion("강남구", 2, city);
        Region dong = persistRegion("역삼동", 3, district);
        RoomType roomType = persistRoomType("원룸");
        RoommateBoard board = persistBoard("상세 게시글", member, roomType, dong,
                LocalDateTime.of(2026, 7, 1, 9, 0), true);

        entityManager.flush();
        entityManager.clear();

        // When
        BasicInfoRow row = roommateBoardRepository.getBasicInfo(board.getId()).orElseThrow();

        // Then
        assertThat(row.boardId()).isEqualTo(board.getId());
        assertThat(row.title()).isEqualTo("상세 게시글");
        assertThat(row.contents()).isEqualTo("테스트 게시글 내용");
        assertThat(row.deposit()).isEqualTo(1_000);
        assertThat(row.managementCost()).isEqualTo(10);
        assertThat(row.monthlyRent()).isEqualTo(50);
        assertThat(row.roomTypeName()).isEqualTo("원룸");
        assertThat(row.regionName()).isEqualTo("역삼동");
        assertThat(row.parentRegionName()).isEqualTo("강남구");
        assertThat(row.grandParentRegionName()).isEqualTo("서울");
        assertThat(row.comeableDateNegotiable()).isTrue();
        assertThat(row.comeableDate()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(row.hits()).isZero();
        assertThat(row.memberId()).isEqualTo(member.getId());
        assertThat(row.memberName()).isEqualTo("상세작성자");
        assertThat(row.memberProfileImageUrl()).isEqualTo("profile-latest.png");
        assertThat(row.birth()).isEqualTo(birth);
        assertThat(row.gender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    @DisplayName("상세 기본 정보 조회는 대상 게시글이 없으면 빈 Optional을 반환한다")
    void getBasicInfoReturnsEmptyWhenBoardDoesNotExist() {
        // When & Then
        assertThat(roommateBoardRepository.getBasicInfo(999L)).isEmpty();
    }

    @Test
    @DisplayName("수정 폼 기본 정보 조회는 게시글과 방 유형 및 지역 계층 원천 데이터를 반환한다")
    void getEditRowReturnsRawEditFormRow() {
        // Given
        Member member = persistMember("provider-edit");
        Region city = persistRegion("서울", 1, null);
        Region district = persistRegion("강남구", 2, city);
        Region dong = persistRegion("역삼동", 3, district);
        RoomType roomType = persistRoomType("원룸");
        persistRoomTypeFile(roomType, persistFile("one-room.png"));
        RoommateBoard board = persistBoard("수정 게시글", member, roomType, dong,
                LocalDateTime.of(2026, 7, 1, 9, 0), true);
        entityManager.flush();
        entityManager.clear();

        // When
        EditFormRow row = roommateBoardRepository.getEditRow(board.getId()).orElseThrow();

        // Then
        assertThat(row.title()).isEqualTo("수정 게시글");
        assertThat(row.contents()).isEqualTo("테스트 게시글 내용");
        assertThat(row.deposit()).isEqualTo(1_000);
        assertThat(row.managementCost()).isEqualTo(10);
        assertThat(row.monthlyRent()).isEqualTo(50);
        assertThat(row.roomTypeId()).isEqualTo(roomType.getId());
        assertThat(row.roomTypeName()).isEqualTo("원룸");
        assertThat(row.roomTypeImageUrl()).isEqualTo("one-room.png");
        assertThat(row.regionId()).isEqualTo(dong.getId());
        assertThat(row.regionName()).isEqualTo("역삼동");
        assertThat(row.parentRegionName()).isEqualTo("강남구");
        assertThat(row.grandParentRegionName()).isEqualTo("서울");
        assertThat(row.comeableDateNegotiable()).isTrue();
        assertThat(row.comeableDate()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    @Test
    @DisplayName("수정 폼 기본 정보 조회는 삭제된 게시글이면 빈 Optional을 반환한다")
    void getEditRowReturnsEmptyWhenBoardIsDeleted() {
        // Given
        Member member = persistMember("provider-deleted-edit");
        Region region = persistRegion("역삼동", 3, null);
        RoomType roomType = persistRoomType("원룸");
        RoommateBoard board = persistBoard("삭제된 수정 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 1, 9, 0));
        ReflectionTestUtils.setField(board, "isDeleted", true);
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertThat(roommateBoardRepository.getEditRow(board.getId())).isEmpty();
    }

    @Test
    @DisplayName("이미지 조회는 대표 이미지를 먼저 반환하고 최대 10개까지만 반환한다")
    void getFileDetailDtoByBoardIdReturnsThumbnailFirstAndLimitsTen() {
        // Given
        Member member = persistMember("provider-detail-images");
        Region region = persistRegion("역삼동", 3, null);
        RoomType roomType = persistRoomType("원룸");
        RoommateBoard board = persistBoard("이미지 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 1, 9, 0));

        for (int i = 1; i <= 11; i++) {
            persistBoardFile(board, persistFile("room-" + i + ".jpg"), false);
        }
        RoommateBoardFile thumbnailBoardFile = persistBoardFile(board, persistFile("thumbnail.jpg"), true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<BoardDetailDto.Response.FileDetailDto> images =
                roommateBoardFileRepository.getFileDetailDtoByBoardId(board.getId());

        // Then
        assertThat(images).hasSize(10);
        assertThat(images.getFirst().getBoardFileId()).isEqualTo(thumbnailBoardFile.getId());
        assertThat(images.getFirst().getUrl()).isEqualTo("thumbnail.jpg");
    }

    @Test
    @DisplayName("게시글 ID 목록의 삭제되지 않은 대표 이미지를 한 번에 조회한다")
    void findThumbnailsByBoardIdsReturnsOnlyActiveThumbnails() {
        // Given
        Member member = persistMember("provider-list-thumbnails");
        Region region = persistRegion("역삼동", 3, null);
        RoomType roomType = persistRoomType("원룸");
        RoommateBoard firstBoard = persistBoard(
                "첫 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 1, 9, 0));
        RoommateBoard secondBoard = persistBoard(
                "두 번째 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 2, 9, 0));
        RoommateBoard excludedBoard = persistBoard(
                "조회 제외 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 3, 9, 0));
        persistBoardFile(firstBoard, persistFile("first-normal.jpg"), false);
        persistBoardFile(firstBoard, persistFile("first-thumbnail.jpg"), true);
        File deletedThumbnail = persistFile("deleted-thumbnail.jpg");
        deletedThumbnail.softDelete();
        persistBoardFile(firstBoard, deletedThumbnail, true);
        persistBoardFile(secondBoard, persistFile("second-thumbnail.jpg"), true);
        persistBoardFile(excludedBoard, persistFile("excluded-thumbnail.jpg"), true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<BoardThumbnailRow> rows = roommateBoardFileRepository.findThumbnailsByBoardIds(
                List.of(firstBoard.getId(), secondBoard.getId()));

        // Then
        assertThat(rows)
                .extracting(BoardThumbnailRow::boardId, BoardThumbnailRow::imageUrl)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(firstBoard.getId(), "first-thumbnail.jpg"),
                        org.assertj.core.groups.Tuple.tuple(secondBoard.getId(), "second-thumbnail.jpg")
                );
    }

    @Test
    @DisplayName("방 추가 옵션 조회는 삭제되지 않은 옵션과 대표 이미지 URL을 반환한다")
    void getExtraOptionsByBoardIdReturnsOnlyActiveOptionInfos() {
        // Given
        Member member = persistMember("provider-detail-options");
        Region region = persistRegion("역삼동", 3, null);
        RoomType roomType = persistRoomType("원룸");
        RoommateBoard board = persistBoard("옵션 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 1, 9, 0));
        RoomExtraOption activeOption = persistRoomExtraOption("풀옵션");
        persistRoomExtraOptionFile(activeOption, persistFile("full-option.png"));
        persistRoommateBoardOption(board, activeOption);
        RoomExtraOption deletedOption = persistRoomExtraOption("삭제된 옵션");
        ReflectionTestUtils.setField(deletedOption, "isDeleted", true);
        persistRoommateBoardOption(board, deletedOption);
        entityManager.flush();
        entityManager.clear();

        // When
        List<RoomExtraOptionInfo> optionInfos =
                roommateBoardOptionRepository.getExtraOptionsByBoardId(board.getId());

        // Then
        assertThat(optionInfos).singleElement()
                .satisfies(optionInfo -> {
                    assertThat(optionInfo.getExtraOptionId()).isEqualTo(activeOption.getId());
                    assertThat(optionInfo.getName()).isEqualTo("풀옵션");
                    assertThat(optionInfo.getImageUrl()).isEqualTo("full-option.png");
                });
    }

    @Test
    @DisplayName("생활패턴 조회는 회원의 생활패턴 정보를 sort 순서대로 반환한다")
    void getLifeStyleDtoReturnsMemberLifePatterns() {
        // Given
        Member member = persistMember("provider-detail-lifestyles");
        LifePattern sleepPattern = persistLifePattern("취침", LifePatternType.SCALE, 2);
        persistLifePatternFile(sleepPattern, persistFile("sleep.png"));
        persistMemberLifePattern(member, persistLifePatternInformation(sleepPattern, "23:00", "일찍 자요"));
        LifePattern visitorPattern = persistLifePattern("방문객", LifePatternType.SINGLE_CHOICE, 1);
        persistLifePatternFile(visitorPattern, persistFile("visitor.png"));
        persistMemberLifePattern(member, persistLifePatternInformation(visitorPattern, "가끔", "가끔 방문해요"));
        entityManager.flush();
        entityManager.clear();

        // When
        List<BoardDetailDto.Response.Lifestyle> lifeStyles =
                memberLifePatternRepository.getLifeStyleDto(member.getId());
        List<MatchingLifestyleRow> matchingRows =
                memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(member.getId()));

        // Then
        assertThat(lifeStyles)
                .extracting(BoardDetailDto.Response.Lifestyle::getName)
                .containsExactly("방문객", "취침");
        assertThat(lifeStyles)
                .filteredOn(lifeStyle -> lifeStyle.getName().equals("취침"))
                .singleElement()
                .satisfies(lifeStyle -> {
                    assertThat(lifeStyle.getLifestyleId()).isNotNull();
                    assertThat(lifeStyle.getValue()).isEqualTo("23:00");
                    assertThat(lifeStyle.getDescription()).isEqualTo("일찍 자요");
                    assertThat(lifeStyle.getType()).isEqualTo(LifePatternType.SCALE);
                    assertThat(lifeStyle.getImageUrl()).isEqualTo("sleep.png");
                });
        assertThat(matchingRows)
                .extracting(MatchingLifestyleRow::name, MatchingLifestyleRow::imageUrl)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("방문객", "visitor.png"),
                        org.assertj.core.groups.Tuple.tuple("취침", "sleep.png")
                );
    }

    @Test
    @DisplayName("선호 룸메이트 조건 조회는 조건의 이름과 표시 정보를 반환한다")
    void getConditionDtoByMemberIdReturnsPreferenceConditions() {
        // Given
        Member member = persistMember("provider-detail-conditions");
        LifePattern smokingPattern = persistLifePattern("흡연", LifePatternType.BOOLEAN);
        persistLifePatternFile(smokingPattern, persistFile("smoking.png"));
        persistPreferenceCondition(member, persistLifePatternInformation(smokingPattern, "비흡연", "비흡연 선호"));
        entityManager.flush();
        entityManager.clear();

        // When
        List<BoardDetailDto.Response.Condition> conditions =
                preferenceConditionRepository.getConditionDtoByMemberId(member.getId());
        List<MatchingPreferenceConditionRow> matchingRows =
                preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(member.getId()));

        // Then
        assertThat(conditions).singleElement()
                .satisfies(condition -> {
                    assertThat(condition.getConditionId()).isNotNull();
                    assertThat(condition.getName()).isEqualTo("흡연");
                    assertThat(condition.getValue()).isEqualTo("비흡연");
                    assertThat(condition.getDescription()).isEqualTo("비흡연 선호");
                    assertThat(condition.getType()).isEqualTo(LifePatternType.BOOLEAN);
                    assertThat(condition.getImageUrl()).isEqualTo("smoking.png");
                });
        assertThat(matchingRows).singleElement()
                .satisfies(row -> assertThat(row.imageUrl()).isEqualTo("smoking.png"));
    }

    @Test
    @DisplayName("중요 조건 조회는 회원이 선택한 중요 조건명을 반환한다")
    void getConditionWeightDtoByMemberIdReturnsPreferenceConditionWeights() {
        // Given
        Member member = persistMember("provider-detail-condition-weights");
        LifePattern sleepPattern = persistLifePattern("취침", LifePatternType.SCALE, 2);
        LifePattern noisePattern = persistLifePattern("소음", LifePatternType.SCALE, 1);
        persistLifePatternFile(sleepPattern, persistFile("sleep.png"));
        persistLifePatternFile(noisePattern, persistFile("noise.png"));
        persistPreferenceConditionWeight(member, sleepPattern);
        persistPreferenceConditionWeight(member, noisePattern);
        entityManager.flush();
        entityManager.clear();

        // When
        List<BoardDetailDto.Response.ConditionWeight> conditionWeights =
                preferenceConditionWeightRepository.getConditionWeightDtoByMemberId(member.getId());
        List<MatchingPreferenceConditionWeightRow> matchingRows =
                preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(
                        List.of(member.getId()));

        // Then
        assertThat(conditionWeights)
                .extracting(BoardDetailDto.Response.ConditionWeight::getName)
                .containsExactlyInAnyOrder("취침", "소음");
        assertThat(conditionWeights)
                .extracting(BoardDetailDto.Response.ConditionWeight::getWeightConditionId)
                .doesNotContainNull();
        assertThat(conditionWeights)
                .extracting(BoardDetailDto.Response.ConditionWeight::getImageUrl)
                .containsExactlyInAnyOrder("sleep.png", "noise.png");
        assertThat(matchingRows)
                .extracting(MatchingPreferenceConditionWeightRow::name,
                        MatchingPreferenceConditionWeightRow::imageUrl)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("취침", "sleep.png"),
                        org.assertj.core.groups.Tuple.tuple("소음", "noise.png")
                );
    }

    @Test
    @DisplayName("승인 인증 조회는 승인되고 삭제되지 않은 인증 타입만 반환한다")
    void getAcceptedAuthenticationTypeByMemberIdReturnsOnlyAcceptedActiveTypes() {
        // Given
        Member member = persistMember("provider-detail-auth");
        persistAuthentication(member, AuthenticationType.STUDENT);
        Authentication notAccepted = persistAuthentication(member, AuthenticationType.COMPANY);
        ReflectionTestUtils.setField(notAccepted, "isAccepted", false);
        Authentication deleted = persistAuthentication(member, AuthenticationType.STUDENT);
        ReflectionTestUtils.setField(deleted, "isDeleted", true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<AuthenticationType> authenticationTypes =
                authenticationRepository.getAcceptedAuthenticationTypeByMemberId(member.getId());

        // Then
        assertThat(authenticationTypes).containsExactly(AuthenticationType.STUDENT);
    }

    @Test
    @DisplayName("회원 ID 목록의 승인되고 삭제되지 않은 인증을 한 번에 조회한다")
    void findAcceptedByMemberIdsReturnsOnlyAcceptedActiveAuthentications() {
        // Given
        Member firstMember = persistMember("provider-list-auth-first");
        Member secondMember = persistMember("provider-list-auth-second");
        Member excludedMember = persistMember("provider-list-auth-excluded");
        persistAuthentication(firstMember, AuthenticationType.STUDENT);
        Authentication notAccepted = persistAuthentication(firstMember, AuthenticationType.COMPANY);
        ReflectionTestUtils.setField(notAccepted, "isAccepted", false);
        persistAuthentication(secondMember, AuthenticationType.COMPANY);
        Authentication deleted = persistAuthentication(secondMember, AuthenticationType.STUDENT);
        ReflectionTestUtils.setField(deleted, "isDeleted", true);
        persistAuthentication(excludedMember, AuthenticationType.STUDENT);
        entityManager.flush();
        entityManager.clear();

        // When
        List<MemberAuthenticationRow> rows = authenticationRepository.findAcceptedByMemberIds(
                List.of(firstMember.getId(), secondMember.getId()));

        // Then
        assertThat(rows)
                .extracting(MemberAuthenticationRow::memberId, MemberAuthenticationRow::type)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(firstMember.getId(), AuthenticationType.STUDENT),
                        org.assertj.core.groups.Tuple.tuple(secondMember.getId(), AuthenticationType.COMPANY)
                );
    }

    @Test
    @DisplayName("조회수 증가 쿼리는 삭제되지 않은 게시글의 조회수를 1 증가시킨다")
    void increaseHitsByIdIncrementsVisibleBoardHits() {
        // Given
        Member member = persistMember("provider-hit-up");
        RoomType roomType = persistRoomType("투룸");
        Region region = persistRegion("방배동", 3, null);
        RoommateBoard board = persistBoard("조회수 증가 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 1, 9, 0));
        entityManager.flush();
        entityManager.clear();

        // When
        int updatedCount = roommateBoardRepository.increaseHitsById(board.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        RoommateBoard foundBoard = entityManager.find(RoommateBoard.class, board.getId());
        assertThat(updatedCount).isEqualTo(1);
        assertThat(foundBoard.getHits()).isEqualTo(1L);
    }

    @Test
    @DisplayName("조회수 증가 쿼리는 삭제된 게시글이면 변경하지 않는다")
    void increaseHitsByIdDoesNotUpdateDeletedBoard() {
        // Given
        Member member = persistMember("provider-deleted-hit-up");
        RoomType roomType = persistRoomType("원룸");
        Region region = persistRegion("논현동", 3, null);
        RoommateBoard board = persistBoard("삭제된 게시글", member, roomType, region, LocalDateTime.of(2026, 7, 1, 9, 0));
        ReflectionTestUtils.setField(board, "isDeleted", true);
        entityManager.flush();
        entityManager.clear();

        // When
        int updatedCount = roommateBoardRepository.increaseHitsById(board.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        RoommateBoard foundBoard = entityManager.find(RoommateBoard.class, board.getId());
        assertThat(updatedCount).isZero();
        assertThat(foundBoard.getHits()).isZero();
    }

    private BoardListDto.Request defaultRequest() {
        return request(null, null, null, null, null, null, null);
    }

    private BoardListDto.Request request(
            List<Long> regionIds,
            List<Long> roomTypeIds,
            Gender gender,
            Integer minDeposit,
            Integer maxDeposit,
            Integer minMounthRent,
            Integer maxMounthRent
    ) {
        BoardListDto.Request request = new BoardListDto.Request();
        request.setRegionIds(regionIds);
        request.setRoomTypeIds(roomTypeIds);
        request.setGender(gender);
        request.setMinDeposit(minDeposit);
        request.setMaxDeposit(maxDeposit);
        request.setMinMounthRent(minMounthRent);
        request.setMaxMounthRent(maxMounthRent);
        return request;
    }

    private Member persistMember(String providerId) {
        return persistMember(providerId, MemberState.ACTIVE);
    }

    private Member persistMember(String providerId, MemberState memberState) {
        Member member = Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId(providerId)
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        entityManager.persist(member);
        entityManager.persist(State.builder()
                .member(member)
                .states(memberState)
                .build());
        return member;
    }

    private void persistBlock(Member blocker, Member blocked, boolean isDeleted) {
        entityManager.persist(Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .isDeleted(isDeleted)
                .build());
    }

    private BasicInformation persistBasicInformation(Member member, String name, Gender gender, String email) {
        return persistBasicInformation(member, name, gender, email, LocalDate.of(1998, 1, 1));
    }

    private BasicInformation persistBasicInformation(Member member, String name, Gender gender, String email, LocalDate birth) {
        BasicInformation basicInformation = BasicInformation.builder()
                .member(member)
                .name(name)
                .birth(birth)
                .gender(gender)
                .email(email)
                .build();
        entityManager.persist(basicInformation);
        return basicInformation;
    }

    private RoomType persistRoomType(String name) {
        RoomType roomType = newInstance(RoomType.class);
        ReflectionTestUtils.setField(roomType, "name", name);
        ReflectionTestUtils.setField(roomType, "isDeleted", false);
        entityManager.persist(roomType);
        return roomType;
    }

    private Region persistRegion(String name, Integer scope, Region parent) {
        Region region = newInstance(Region.class);
        ReflectionTestUtils.setField(region, "name", name);
        ReflectionTestUtils.setField(region, "scope", scope);
        ReflectionTestUtils.setField(region, "parent", parent);
        entityManager.persist(region);
        return region;
    }

    private RoommateBoard persistBoard(
            String title,
            Member member,
            RoomType roomType,
            Region region,
            LocalDateTime comeableDate
    ) {
        return persistBoard(title, member, roomType, region, comeableDate, false);
    }

    private RoommateBoard persistBoard(
            String title,
            Member member,
            RoomType roomType,
            Region region,
            LocalDateTime comeableDate,
            Boolean comeableDateNegotiable
    ) {
        RoommateBoard board = RoommateBoard.builder()
                .member(member)
                .title(title)
                .contents("테스트 게시글 내용")
                .deposit(1_000)
                .monthlyRent(50)
                .managementCost(10)
                .roomType(roomType)
                .region(region)
                .comeableDateNegotiable(comeableDateNegotiable)
                .comeableDate(comeableDate)
                .build();
        entityManager.persist(board);
        return board;
    }

    private File persistFile(String savedFileName) {
        File file = File.builder()
                .type(FileType.ROOMMATE_BOARD_IMAGE)
                .originalFileName(savedFileName)
                .savedFileName(savedFileName)
                .fileExt("jpg")
                .build();
        entityManager.persist(file);
        return file;
    }

    private RoomTypeFile persistRoomTypeFile(RoomType roomType, File file) {
        RoomTypeFile roomTypeFile = RoomTypeFile.builder()
                .roomType(roomType)
                .file(file)
                .build();
        entityManager.persist(roomTypeFile);
        return roomTypeFile;
    }

    private RoomExtraOptionFile persistRoomExtraOptionFile(RoomExtraOption roomExtraOption, File file) {
        RoomExtraOptionFile roomExtraOptionFile = RoomExtraOptionFile.builder()
                .roomExtraOption(roomExtraOption)
                .file(file)
                .build();
        entityManager.persist(roomExtraOptionFile);
        return roomExtraOptionFile;
    }

    private LifePatternFile persistLifePatternFile(LifePattern lifePattern, File file) {
        LifePatternFile lifePatternFile = LifePatternFile.builder()
                .lifePattern(lifePattern)
                .file(file)
                .build();
        entityManager.persist(lifePatternFile);
        return lifePatternFile;
    }

    private BasicInformationFile persistBasicInformationFile(BasicInformation basicInformation, File file) {
        BasicInformationFile basicInformationFile = newInstance(BasicInformationFile.class);
        ReflectionTestUtils.setField(basicInformationFile, "basicInformation", basicInformation);
        ReflectionTestUtils.setField(basicInformationFile, "file", file);
        entityManager.persist(basicInformationFile);
        return basicInformationFile;
    }

    private RoommateBoardFile persistBoardFile(RoommateBoard board, File file, boolean isThumbnail) {
        RoommateBoardFile roommateBoardFile = RoommateBoardFile.builder()
                .roommateBoard(board)
                .file(file)
                .isThumbnail(isThumbnail)
                .build();
        entityManager.persist(roommateBoardFile);
        return roommateBoardFile;
    }

    private RoomExtraOption persistRoomExtraOption(String name) {
        RoomExtraOption option = newInstance(RoomExtraOption.class);
        ReflectionTestUtils.setField(option, "name", name);
        ReflectionTestUtils.setField(option, "isDeleted", false);
        entityManager.persist(option);
        return option;
    }

    private RoommateBoardOption persistRoommateBoardOption(RoommateBoard board, RoomExtraOption option) {
        RoommateBoardOption roommateBoardOption = newInstance(RoommateBoardOption.class);
        ReflectionTestUtils.setField(roommateBoardOption, "roommateBoard", board);
        ReflectionTestUtils.setField(roommateBoardOption, "roomExtraOption", option);
        entityManager.persist(roommateBoardOption);
        return roommateBoardOption;
    }

    private LifePattern persistLifePattern(String name, LifePatternType type) {
        return persistLifePattern(name, type, 0);
    }

    private LifePattern persistLifePattern(String name, LifePatternType type, Integer sort) {
        LifePattern pattern = newInstance(LifePattern.class);
        ReflectionTestUtils.setField(pattern, "name", name);
        ReflectionTestUtils.setField(pattern, "dtype", type);
        ReflectionTestUtils.setField(pattern, "isDeleted", false);
        ReflectionTestUtils.setField(pattern, "sort", sort);
        ReflectionTestUtils.setField(pattern, "lifePatternDescription", "생활패턴 설명");
        ReflectionTestUtils.setField(pattern, "preferenceDescription", "선호조건 설명");
        entityManager.persist(pattern);
        return pattern;
    }

    private LifePatternInformation persistLifePatternInformation(LifePattern pattern, String value, String description) {
        LifePatternInformation information = newInstance(LifePatternInformation.class);
        ReflectionTestUtils.setField(information, "lifePattern", pattern);
        ReflectionTestUtils.setField(information, "dvalue", value);
        ReflectionTestUtils.setField(information, "description", description);
        entityManager.persist(information);
        return information;
    }

    private MemberLifePattern persistMemberLifePattern(Member member, LifePatternInformation information) {
        MemberLifePattern memberLifePattern = MemberLifePattern.builder()
                .member(member)
                .lifePatternInformation(information)
                .build();
        entityManager.persist(memberLifePattern);
        return memberLifePattern;
    }

    private PreferenceCondition persistPreferenceCondition(Member member, LifePatternInformation information) {
        PreferenceCondition preferenceCondition = newInstance(PreferenceCondition.class);
        ReflectionTestUtils.setField(preferenceCondition, "member", member);
        ReflectionTestUtils.setField(preferenceCondition, "lifePatternInformation", information);
        entityManager.persist(preferenceCondition);
        return preferenceCondition;
    }

    private PreferenceConditionWeight persistPreferenceConditionWeight(Member member, LifePattern pattern) {
        PreferenceConditionWeight preferenceConditionWeight = PreferenceConditionWeight.builder()
                .member(member)
                .lifePattern(pattern)
                .build();
        entityManager.persist(preferenceConditionWeight);
        return preferenceConditionWeight;
    }

    private Authentication persistAuthentication(Member member, AuthenticationType type) {
        Authentication authentication = newInstance(Authentication.class);
        ReflectionTestUtils.setField(authentication, "member", member);
        ReflectionTestUtils.setField(authentication, "type", type);
        ReflectionTestUtils.setField(authentication, "email", "auth@example.com");
        ReflectionTestUtils.setField(authentication, "code", "123456");
        ReflectionTestUtils.setField(authentication, "isAccepted", true);
        ReflectionTestUtils.setField(authentication, "isDeleted", false);
        entityManager.persist(authentication);
        return authentication;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", e);
        }
    }
}
