package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;
import org.example.knockin.board.dto.BoBoardDetailDto;
import org.example.knockin.board.dto.BoBoardListDto;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.example.knockin.meta.service.impl.AlarmServiceImpl;
import org.example.knockin.meta.service.impl.PushNotificationServiceImpl;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.board.dto.BoardDetailDto;
import org.example.knockin.board.dto.BoardDto;
import org.example.knockin.board.dto.BoardEditDto;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.board.dto.BoardModifyDto;
import org.example.knockin.board.service.impl.RoommateBoardFileServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardInterestServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardOptionServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardServiceImpl;
import org.example.knockin.global.config.RoommateBoardPolicy;
import org.example.knockin.board.dto.BoardDetailDto.Response.RoomExtraOptionInfo;
import org.example.knockin.board.dto.BoardDto.Request.FileDto;
import org.example.knockin.board.dto.BoardModifyDto.Request.ExistingFileDto;
import org.example.knockin.board.dto.BoardModifyDto.Request.NewFileDto;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.declaration.dto.ReportDto;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardBadgeType;
import org.example.knockin.declaration.entity.RoommateBoardDeclaration;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.example.knockin.board.entity.RoommateBoardOption;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.member.entity.Member;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.meta.service.impl.SearchServiceImpl;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.MetaErrorCode;
import org.example.knockin.global.exception.RoommateBoardErrorCode;
import org.example.knockin.global.exception.RoomTypeErrorCode;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.declaration.repository.RoommateBoardDeclarationRepository;
import org.example.knockin.board.repository.RoommateBoardFileRepository;
import org.example.knockin.board.repository.RoommateBoardInterestRepository;
import org.example.knockin.board.repository.RoommateBoardOptionRepository;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.board.repository.row.BasicInfoRow;
import org.example.knockin.board.repository.row.BoardBaseRow;
import org.example.knockin.board.repository.row.BoardThumbnailRow;
import org.example.knockin.board.repository.row.EditFormRow;
import org.example.knockin.meta.repository.FileRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.example.knockin.board.repository.row.BoardInterestCountRow;
import org.example.knockin.meta.service.FileService;
import org.example.knockin.util.service.RoommateScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.declaration.repository.MemberDeclarationRepository;
import org.example.knockin.room.service.impl.RoomExtraOptionServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 게시글 서비스")
class RoommateBoardServiceImplTest {

    @Mock
    private RoommateBoardRepository roommateBoardRepository;

    @Mock
    private RoommateBoardFileRepository roommateBoardFileRepository;

    @Mock
    private RoommateBoardOptionRepository roommateBoardOptionRepository;

    @Mock
    private RoommateBoardInterestRepository roommateBoardInterestRepository;

    @Mock
    private RoommateBoardDeclarationRepository roommateBoardDeclarationRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private MemberServiceImpl memberService;

    @Mock
    private FileService fileService;

    @Mock
    private MetaServiceImpl metaService;

    @Mock
    private RoommateScoreService roommateScoreService;

    private RoommateBoardFileServiceImpl roommateBoardFileService;

    @Mock
    private PreferenceConditionServiceImpl preferenceConditionService;

    @Mock
    private MemberLifePatternService memberLifePatternService;

    @Mock
    private AuthenticationServiceImpl authenticationService;

    private RoommateBoardOptionServiceImpl roommateBoardOptionService;

    private RoommateBoardInterestServiceImpl roommateBoardInterestService;

    private RoommateBoardPolicy roommateBoardPolicy;

    @Mock
    private SearchServiceImpl searchServiceImpl;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    private RoommateBoardServiceImpl roommateBoardService;

    @Mock
    private RoomExtraOptionServiceImpl roomExtraOptionService;

    @BeforeEach
    void setUp() {
        // Keep the existing repository-level scenarios across the extracted board services.
        roommateBoardFileService = spy(new RoommateBoardFileServiceImpl(roommateBoardFileRepository, fileService));
        roommateBoardOptionService = spy(new RoommateBoardOptionServiceImpl(roomExtraOptionService, roommateBoardOptionRepository));
        roommateBoardInterestService = spy(new RoommateBoardInterestServiceImpl(roommateBoardInterestRepository));
        DeclarationServiceImpl declarationService = new DeclarationServiceImpl(
                mock(MemberDeclarationRepository.class), roommateBoardDeclarationRepository, memberService);
        roommateBoardPolicy = new RoommateBoardPolicy();
        roommateBoardPolicy.setImageMaxCount(10);
        roommateBoardPolicy.setThumbnailImageMaxCount(1);
        roommateBoardPolicy.setComeableDateVisibleGraceDays(7);
        roommateBoardPolicy.setHotBadgeMinInterestCount(10);
        roommateBoardService = new RoommateBoardServiceImpl(
                roommateBoardRepository, memberService, metaService, roommateScoreService,
                roommateBoardFileService, preferenceConditionService, memberLifePatternService,
                authenticationService, roommateBoardOptionService, roommateBoardInterestService,
                roommateBoardPolicy, searchServiceImpl, alarmService, pushNotificationService, declarationService);
    }

    @Captor
    private ArgumentCaptor<RoommateBoard> roommateBoardCaptor;

    @Captor
    private ArgumentCaptor<Iterable<File>> filesCaptor;

    @Captor
    private ArgumentCaptor<Iterable<RoommateBoardFile>> boardFilesCaptor;

    @Captor
    private ArgumentCaptor<RoommateBoardFile> boardFileCaptor;

    @Captor
    private ArgumentCaptor<Iterable<RoommateBoardOption>> boardOptionsCaptor;

    @Captor
    private ArgumentCaptor<List<File>> uploadedFilesCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> endDateCaptor;

    @Captor
    private ArgumentCaptor<RoommateBoardInterest> roommateBoardInterestCaptor;

    @Captor
    private ArgumentCaptor<RoommateBoardDeclaration> roommateBoardDeclarationCaptor;

    @Test
    @DisplayName("좋아요 이력이 없으면 관심 게시글을 새로 저장한다")
    void likeBoardCreatesInterestWhenNotExists() {
        Long boardId = 1L;
        Long memberId = 2L;
        RoommateBoard board = createRoommateBoard(10L);
        Member member = Member.builder().id(memberId).build();

        when(roommateBoardRepository.findByIdForUpdate(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(roommateBoardInterestRepository.findByRoommateBoardAndMember(board, member)).thenReturn(Optional.empty());

        BoardDto.Response response = roommateBoardService.likeBoard(boardId, memberId);

        verify(roommateBoardInterestRepository).save(roommateBoardInterestCaptor.capture());
        RoommateBoardInterest interest = roommateBoardInterestCaptor.getValue();
        assertThat(interest.getRoommateBoard()).isSameAs(board);
        assertThat(interest.getMember()).isSameAs(member);
        assertThat(interest.getIsDeleted()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 좋아요한 게시글이면 기존 관심 게시글을 삭제 상태로 토글한다")
    void likeBoardTogglesExistingActiveInterestToDeleted() {
        Long boardId = 1L;
        Long memberId = 2L;
        RoommateBoard board = createRoommateBoard(10L);
        Member member = Member.builder().id(memberId).build();
        RoommateBoardInterest interest = RoommateBoardInterest.builder()
                .roommateBoard(board)
                .member(member)
                .isDeleted(false)
                .build();

        when(roommateBoardRepository.findByIdForUpdate(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(roommateBoardInterestRepository.findByRoommateBoardAndMember(board, member)).thenReturn(Optional.of(interest));

        roommateBoardService.likeBoard(boardId, memberId);

        assertThat(interest.getIsDeleted()).isTrue();
        verify(roommateBoardInterestRepository, never()).save(any(RoommateBoardInterest.class));
    }

    @Test
    @DisplayName("삭제 상태의 관심 게시글이면 다시 좋아요 상태로 토글한다")
    void likeBoardTogglesExistingDeletedInterestToActive() {
        Long boardId = 1L;
        Long memberId = 2L;
        RoommateBoard board = createRoommateBoard(10L);
        Member member = Member.builder().id(memberId).build();
        RoommateBoardInterest interest = RoommateBoardInterest.builder()
                .roommateBoard(board)
                .member(member)
                .isDeleted(true)
                .build();

        when(roommateBoardRepository.findByIdForUpdate(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(roommateBoardInterestRepository.findByRoommateBoardAndMember(board, member)).thenReturn(Optional.of(interest));

        roommateBoardService.likeBoard(boardId, memberId);

        assertThat(interest.getIsDeleted()).isFalse();
        verify(roommateBoardInterestRepository, never()).save(any(RoommateBoardInterest.class));
    }

    @Test
    @DisplayName("같은 좋아요 요청이 두 번 들어오면 토글 구현은 좋아요를 취소한다")
    void likeBoardCancelsLikeWhenSameRequestIsRetried() {
        Long boardId = 1L;
        Long memberId = 2L;
        RoommateBoard board = createRoommateBoard(10L);
        Member member = Member.builder().id(memberId).build();
        AtomicReference<RoommateBoardInterest> savedInterest = new AtomicReference<>();

        when(roommateBoardRepository.findByIdForUpdate(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(roommateBoardInterestRepository.findByRoommateBoardAndMember(board, member))
                .thenAnswer(invocation -> Optional.ofNullable(savedInterest.get()));
        when(roommateBoardInterestRepository.save(any(RoommateBoardInterest.class))).thenAnswer(invocation -> {
            RoommateBoardInterest interest = invocation.getArgument(0);
            savedInterest.set(interest);
            return interest;
        });

        roommateBoardService.likeBoard(boardId, memberId);
        assertThat(savedInterest.get().getIsDeleted()).isFalse();

        roommateBoardService.likeBoard(boardId, memberId);

        assertThat(savedInterest.get().getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("좋아요할 게시글이 없으면 예외를 던진다")
    void likeBoardThrowsWhenBoardNotFound() {
        Long boardId = 1L;

        when(roommateBoardRepository.findByIdForUpdate(boardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateBoardService.likeBoard(boardId, 2L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verifyNoInteractions(roommateBoardInterestRepository);
    }

    @Test
    @DisplayName("좋아요할 회원이 없으면 예외를 던진다")
    void likeBoardThrowsWhenMemberNotFound() {
        Long boardId = 1L;
        Long memberId = 2L;
        RoommateBoard board = createRoommateBoard(10L);

        when(roommateBoardRepository.findByIdForUpdate(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateBoardService.likeBoard(boardId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(roommateBoardInterestRepository);
    }

    @Test
    @DisplayName("게시글 작성자가 삭제를 요청하면 게시글을 삭제 상태로 변경한다")
    void deleteBoardSoftDeletesWhenRequesterIsOwner() {
        // Given
        Long boardId = 1L;
        Long memberId = 7L;
        RoommateBoard board = createRoommateBoard(memberId);
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(board));

        // When
        BoardDto.Response response = roommateBoardService.deleteBoard(boardId, memberId);

        // Then
        assertThat(board.getIsDeleted()).isTrue();
        verify(roommateBoardRepository).save(board);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 회원의 게시글을 삭제하면 권한 없음 예외를 던지고 삭제하지 않는다")
    void deleteBoardThrowsWhenRequesterIsNotOwner() {
        // Given
        Long boardId = 1L;
        Long ownerId = 7L;
        Long requesterId = 999L;
        RoommateBoard board = createRoommateBoard(ownerId);
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(board));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.deleteBoard(boardId, requesterId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FORBIDDEN));
        assertThat(board.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 삭제하면 게시글 없음 예외를 던진다")
    void deleteBoardThrowsWhenBoardDoesNotExist() {
        // Given
        Long boardId = 999L;
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.deleteBoard(boardId, 7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
    }

    @Test
    @DisplayName("게시글 신고를 요청하면 신고 회원과 게시글 및 사유를 저장한다")
    void reportBoardSavesDeclarationWithMemberBoardAndReason() {
        // Given
        Long boardId = 1L;
        Long memberId = 7L;
        ReportDto.Request request = createReportRequest("부적절한 게시글입니다.");
        RoommateBoard board = createRoommateBoard(10L);
        Member member = Member.builder().id(memberId).build();

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(roommateBoardDeclarationRepository.findByRoommateBoardAndMember(board, member))
                .thenReturn(Optional.empty());

        // When
        ReportDto.Response response = roommateBoardService.reportBoard(request, boardId, memberId);

        // Then
        verify(roommateBoardDeclarationRepository).save(roommateBoardDeclarationCaptor.capture());
        RoommateBoardDeclaration declaration = roommateBoardDeclarationCaptor.getValue();
        assertThat(declaration.getRoommateBoard()).isSameAs(board);
        assertThat(declaration.getMember()).isSameAs(member);
        assertThat(declaration.getReason()).isEqualTo("부적절한 게시글입니다.");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 신고한 게시글을 다시 신고하면 중복 신고 예외를 던지고 저장하지 않는다")
    void reportBoardThrowsWhenDeclarationAlreadyExists() {
        // Given
        Long boardId = 1L;
        Long memberId = 7L;
        RoommateBoard board = createRoommateBoard(10L);
        Member member = Member.builder().id(memberId).build();
        RoommateBoardDeclaration existingDeclaration = RoommateBoardDeclaration.builder()
                .roommateBoard(board)
                .member(member)
                .reason("기존 신고 사유")
                .build();

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(roommateBoardDeclarationRepository.findByRoommateBoardAndMember(board, member))
                .thenReturn(Optional.of(existingDeclaration));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.reportBoard(createReportRequest("다시 신고합니다."), boardId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_DECLARATION_DUPLICATE));
        verify(roommateBoardDeclarationRepository, never()).save(any(RoommateBoardDeclaration.class));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 신고하면 게시글 없음 예외를 던지고 회원을 조회하지 않는다")
    void reportBoardThrowsWhenBoardDoesNotExist() {
        // Given
        Long boardId = 999L;
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.reportBoard(createReportRequest("신고 사유"), boardId, 7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verifyNoInteractions(roommateBoardDeclarationRepository);
        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("존재하지 않는 회원이 신고하면 회원 없음 예외를 던지고 신고를 저장하지 않는다")
    void reportBoardThrowsWhenMemberDoesNotExist() {
        // Given
        Long boardId = 1L;
        Long memberId = 7L;
        RoommateBoard board = createRoommateBoard(10L);

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(board));
        when(memberService.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.reportBoard(createReportRequest("신고 사유"), boardId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(roommateBoardDeclarationRepository);
    }

    @Test
    @DisplayName("이미지 업로드가 완료되면 트랜잭션 안에서 게시글과 파일 연결 정보를 저장한다")
    void savePersistsBoardAndUploadedImageLinksInTransaction() throws IOException {
        MultipartFile thumbnailImage = emptyMultipartFile();
        MultipartFile roomImage = emptyMultipartFile();
        BoardDto.Request request = createRequest(
                createFileDto(0, true),
                createFileDto(1, false));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        File thumbnailFile = createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg");
        File roomFile = createFile("room.png", "saved-room.png", "png");
        RoommateBoard savedRoommateBoard = org.mockito.Mockito.mock(RoommateBoard.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 4, 16, 30);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));
        when(fileService.save(thumbnailImage, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(thumbnailFile);
        when(fileService.save(roomImage, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(roomFile);
        when(roommateBoardRepository.save(any(RoommateBoard.class))).thenReturn(savedRoommateBoard);
        when(savedRoommateBoard.getUpdatedAt()).thenReturn(updatedAt);

        BoardDto.Response response = roommateBoardService.save(request, memberId, List.of(thumbnailImage, roomImage));

        verify(roommateBoardRepository).save(roommateBoardCaptor.capture());
        RoommateBoard boardToSave = roommateBoardCaptor.getValue();
        assertThat(boardToSave.getMember()).isSameAs(member);
        assertThat(boardToSave.getTitle()).isEqualTo("Looking for a roommate");
        assertThat(boardToSave.getContents()).isEqualTo("Quiet home near the station");
        assertThat(boardToSave.getDeposit()).isEqualTo(10_000);
        assertThat(boardToSave.getMonthlyRent()).isEqualTo(500);
        assertThat(boardToSave.getManagementCost()).isEqualTo(50);
        assertThat(boardToSave.getRoomType()).isSameAs(roomType);
        assertThat(boardToSave.getRegion()).isSameAs(region);
        assertThat(boardToSave.getComeableDateNegotiable()).isEqualTo(request.getComeableDateNegotiable());
        assertThat(boardToSave.getComeableDate()).isEqualTo(request.getComeableDate());
        assertThat(boardToSave.getIsDeleted()).isFalse();
        assertThat(boardToSave.getHits()).isZero();

        ArgumentCaptor<List<MultipartFile>> imageFilesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Boolean>> thumbnailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(roommateBoardFileService).saveAll(eq(savedRoommateBoard), imageFilesCaptor.capture(), thumbnailsCaptor.capture());
        assertThat(imageFilesCaptor.getValue()).hasSize(2);
        assertThat(imageFilesCaptor.getValue())
                .containsExactly(thumbnailImage, roomImage);
        assertThat(thumbnailsCaptor.getValue()).containsExactly(true, false);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("게시글 저장 이미지가 10장을 초과하면 파일 개수 초과 예외를 던지고 게시글을 저장하지 않는다")
    void saveThrowsWhenImageCountExceedsLimit() throws IOException {
        // Given
        List<FileDto> images = new ArrayList<>();
        for (int index = 0; index <= roommateBoardPolicy.getImageMaxCount(); index++) {
            images.add(createFileDto(index, index == 0));
        }
        BoardDto.Request request = createRequest();
        request.setImages(images);
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_EXCEEDED));
        verifyNoInteractions(fileService, roommateBoardRepository);
        verify(roommateBoardFileService, never()).saveAll(any(RoommateBoard.class), any(), any());
    }

    @Test
    @DisplayName("이미지가 있지만 썸네일이 없으면 썸네일 개수 예외를 던지고 게시글을 저장하지 않는다")
    void saveThrowsWhenThumbnailIsMissing() throws IOException {
        // Given
        BoardDto.Request request = createRequest(
                createFileDto(0, false),
                createFileDto(1, false));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_THUMBNAIL_EXCEEDED));
        verifyNoInteractions(fileService, roommateBoardRepository);
        verify(roommateBoardFileService, never()).saveAll(any(RoommateBoard.class), any(), any());
    }

    @Test
    @DisplayName("썸네일이 2장이면 썸네일 개수 예외를 던지고 게시글을 저장하지 않는다")
    void saveThrowsWhenMultipleThumbnailsAreRequested() throws IOException {
        // Given
        BoardDto.Request request = createRequest(
                createFileDto(0, true),
                createFileDto(1, true));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_THUMBNAIL_EXCEEDED));
        verifyNoInteractions(fileService, roommateBoardRepository);
        verify(roommateBoardFileService, never()).saveAll(any(RoommateBoard.class), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 저장을 요청하면 이미지 업로드와 트랜잭션 저장을 수행하지 않는다")
    void saveThrowsWhenMemberDoesNotExist() {
        BoardDto.Request request = createRequest(createFileDto(0, true));
        Long memberId = 42L;
        when(memberService.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(emptyMultipartFile())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

        verifyNoInteractions(metaService, fileService, roommateBoardRepository, roommateBoardFileRepository);
    }

    @Test
    @DisplayName("존재하지 않는 방 유형으로 저장을 요청하면 이미지 업로드와 트랜잭션 저장을 수행하지 않는다")
    void saveThrowsWhenRoomTypeDoesNotExist() {
        BoardDto.Request request = createRequest(createFileDto(0, true));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenThrow(new BusinessException(RoomTypeErrorCode.ROOM_TYPE_NOT_FOUND));
 
        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(emptyMultipartFile())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomTypeErrorCode.ROOM_TYPE_NOT_FOUND));
 
        verifyNoInteractions(fileService, roommateBoardRepository, roommateBoardFileRepository);
    }

    @Test
    @DisplayName("존재하지 않는 지역으로 저장을 요청하면 이미지 업로드와 트랜잭션 저장을 수행하지 않는다")
    void saveThrowsWhenRegionDoesNotExist() {
        BoardDto.Request request = createRequest(createFileDto(0, true));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(emptyMultipartFile())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MetaErrorCode.REGION_NOT_FOUND));

        verifyNoInteractions(fileService, roommateBoardRepository, roommateBoardFileRepository);
    }

    @Test
    @DisplayName("이미지 업로드가 실패하면 업로드 실패 예외를 던진다")
    void saveThrowsWhenFileUploadFails() throws IOException {
        MultipartFile thumbnailImage = emptyMultipartFile();
        BoardDto.Request request = createRequest(createFileDto(0, true));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));
        when(fileService.save(thumbnailImage, FileType.ROOMMATE_BOARD_IMAGE))
                .thenThrow(new IOException("upload failed"));

        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(thumbnailImage)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_FAILED));

        verify(roommateBoardRepository).save(any(RoommateBoard.class));
        verify(fileService, never()).deleteAll(any());
        verify(roommateBoardFileRepository, never()).save(any(RoommateBoardFile.class));
    }

    @Test
    @DisplayName("두 번째 이미지 업로드가 실패하면 업로드 실패 예외를 던진다")
    void saveDeletesUploadedImagesWhenLaterUploadFails() throws IOException {
        MultipartFile thumbnailImage = emptyMultipartFile();
        MultipartFile roomImage = emptyMultipartFile();
        BoardDto.Request request = createRequest(
                createFileDto(0, true),
                createFileDto(1, false));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        File thumbnailFile = createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg");
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));
        when(fileService.save(thumbnailImage, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(thumbnailFile);
        when(fileService.save(roomImage, FileType.ROOMMATE_BOARD_IMAGE))
                .thenThrow(new IOException("upload failed"));

        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(thumbnailImage, roomImage)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_FAILED));

        verify(roommateBoardRepository).save(any(RoommateBoard.class));
        verify(fileService, never()).deleteAll(any());
    }

    @Test
    @DisplayName("게시글 저장이 실패하면 파일 저장을 수행하지 않고 예외를 다시 던진다")
    void saveDeletesUploadedImagesWhenTransactionalSaveFails() throws IOException {
        MultipartFile thumbnailImage = emptyMultipartFile();
        BoardDto.Request request = createRequest(createFileDto(0, true));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));
        when(roommateBoardRepository.save(any(RoommateBoard.class))).thenThrow(new IllegalStateException("db failed"));

        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(thumbnailImage)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db failed");

        verify(fileService, never()).save(any(MultipartFile.class), any(FileType.class));
        verify(fileService, never()).deleteAll(any());
        verify(roommateBoardFileService, never()).saveAll(any(RoommateBoard.class), any(), any());
    }

    @Test
    @DisplayName("이미지 없이 저장을 요청하면 게시글만 저장하고 파일 업로드를 수행하지 않는다")
    void saveAllowsRequestWithoutImages() throws IOException {
        BoardDto.Request request = createRequest();
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoard savedRoommateBoard = org.mockito.Mockito.mock(RoommateBoard.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 4, 16, 30);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));
        when(roommateBoardRepository.save(any(RoommateBoard.class))).thenReturn(savedRoommateBoard);
        when(savedRoommateBoard.getUpdatedAt()).thenReturn(updatedAt);

        BoardDto.Response response = roommateBoardService.save(request, memberId, null);

        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        verifyNoInteractions(fileService);
        verify(roommateBoardFileService, never()).saveAll(any(RoommateBoard.class), any(), any());
        verifyNoInteractions(fileRepository, roommateBoardFileRepository);
    }

    @Test
    @DisplayName("입주 시기 협의 가능이면 입주 가능일 없이도 게시글을 저장한다")
    void saveAllowsNegotiableComeableDateWithoutDate() {
        // Given
        BoardDto.Request request = createRequest();
        request.setComeableDateNegotiable(true);
        request.setComeableDate(null);
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoard savedRoommateBoard = org.mockito.Mockito.mock(RoommateBoard.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 4, 16, 30);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));
        when(roommateBoardRepository.save(any(RoommateBoard.class))).thenReturn(savedRoommateBoard);
        when(savedRoommateBoard.getUpdatedAt()).thenReturn(updatedAt);

        // When
        BoardDto.Response response = roommateBoardService.save(request, memberId, null);

        // Then
        verify(roommateBoardRepository).save(roommateBoardCaptor.capture());
        RoommateBoard boardToSave = roommateBoardCaptor.getValue();
        assertThat(boardToSave.getComeableDateNegotiable()).isTrue();
        assertThat(boardToSave.getComeableDate()).isNull();
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("게시글 저장 중 이미지 메타데이터가 파일 파트와 매칭되지 않으면 잘못된 요청 예외를 던진다")
    void saveThrowsWhenImageFileIndexDoesNotMatchFilesPart() throws IOException {
        BoardDto.Request request = createRequest(createFileDto(1, true));
        Long memberId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);

        when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        when(metaService.findByRoomTypeId(1L)).thenReturn(roomType);
        when(metaService.findByRegionId(2L)).thenReturn(Optional.of(region));

        assertThatThrownBy(() -> roommateBoardService.save(request, memberId, List.of(emptyMultipartFile())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        verify(fileService, never()).save(any(MultipartFile.class), any(FileType.class));
        verifyNoInteractions(roommateBoardRepository, fileRepository, roommateBoardFileRepository);
    }

    @Test
    @DisplayName("목록 조회 시 입주 가능시기 노출 기준을 현재 시각 기준 7일 전으로 전달한다")
    void getBoardListPassesComeableDateGraceEndDate() {
        BoardListDto.Request request = new BoardListDto.Request();
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime beforeEndDate = LocalDateTime.now()
                .minusDays(roommateBoardPolicy.getComeableDateVisibleGraceDays());
        when(roommateBoardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(null)))
                .thenReturn(new SliceImpl<>(List.of(), pageable, false));

        Slice<BoardListDto.Response> result = roommateBoardService.getBoardList(request, pageable, null);

        LocalDateTime afterEndDate = LocalDateTime.now()
                .minusDays(roommateBoardPolicy.getComeableDateVisibleGraceDays());
        verify(roommateBoardRepository).search(eq(request), eq(pageable), endDateCaptor.capture(), eq(null));
        assertThat(endDateCaptor.getValue()).isBetween(beforeEndDate, afterEndDate);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getPageable()).isEqualTo(pageable);
        verifyNoInteractions(roommateBoardFileService, authenticationService);
    }

    @Test
    @DisplayName("로그인 사용자의 키워드는 검색 결과가 없어도 저장한다")
    void getBoardListSavesKeywordForAuthenticatedRequesterEvenWhenResultIsEmpty() {
        // Given
        Long requesterId = 42L;
        Member member = org.mockito.Mockito.mock(Member.class);
        BoardListDto.Request request = new BoardListDto.Request();
        request.setKeyword("  원룸  ");
        Pageable pageable = PageRequest.of(0, 20);
        when(memberService.findByIdOrThrow(requesterId)).thenReturn(member);
        when(roommateBoardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(requesterId)))
                .thenReturn(new SliceImpl<>(List.of(), pageable, false));

        // When
        roommateBoardService.getBoardList(request, pageable, requesterId);

        // Then
        verify(memberService).findByIdOrThrow(requesterId);
        verify(searchServiceImpl).save(member, "  원룸  ");
    }

    @Test
    @DisplayName("비로그인 사용자의 키워드는 검색 이력으로 저장하지 않는다")
    void getBoardListDoesNotSaveKeywordForAnonymousRequester() {
        // Given
        BoardListDto.Request request = new BoardListDto.Request();
        request.setKeyword("원룸");
        Pageable pageable = PageRequest.of(0, 20);
        when(roommateBoardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(null)))
                .thenReturn(new SliceImpl<>(List.of(), pageable, false));

        // When
        roommateBoardService.getBoardList(request, pageable, null);

        // Then
        verifyNoInteractions(memberService, searchServiceImpl);
    }

    @Test
    @DisplayName("키워드가 null 또는 공백이면 로그인 사용자도 검색 이력을 저장하지 않는다")
    void getBoardListDoesNotSaveNullOrBlankKeyword() {
        // Given
        Long requesterId = 42L;
        BoardListDto.Request request = new BoardListDto.Request();
        Pageable pageable = PageRequest.of(0, 20);
        when(roommateBoardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(requesterId)))
                .thenReturn(new SliceImpl<>(List.of(), pageable, false));

        // When
        request.setKeyword(null);
        roommateBoardService.getBoardList(request, pageable, requesterId);
        request.setKeyword("   ");
        roommateBoardService.getBoardList(request, pageable, requesterId);

        // Then
        verifyNoInteractions(memberService, searchServiceImpl);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("목록 조회는 게시글 응답을 조합하면서 Slice의 다음 페이지 여부와 조회 정보를 유지한다")
    void getBoardListComposesResponseInService(boolean hasNext) {
        // Given
        BoardListDto.Request request = new BoardListDto.Request();
        Pageable pageable = PageRequest.of(1, 1);
        LocalDateTime comeableDate = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 25, 9, 0);
        LocalDate memberBirth = LocalDate.of(1998, 4, 15);
        BoardBaseRow baseRow = new BoardBaseRow(
                1L,
                "룸메이트를 구합니다",
                1_000,
                50,
                10,
                comeableDate,
                7L,
                "원룸",
                "역삼동",
                "강남구",
                "서울",
                11L,
                "작성자",
                "profile.jpg",
                memberBirth,
                Gender.FEMALE,
                createdAt
        );
        when(roommateBoardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(null)))
                .thenReturn(new SliceImpl<>(List.of(baseRow), pageable, hasNext));
        when(roommateBoardFileService.findThumbnailsByBoardIds(List.of(1L)))
                .thenReturn(List.of(new BoardThumbnailRow(1L, "thumbnail.jpg")));
        when(authenticationService.findAcceptedByMemberIds(List.of(11L)))
                .thenReturn(List.of(
                        new MemberAuthenticationRow(11L, AuthenticationType.STUDENT),
                        new MemberAuthenticationRow(11L, AuthenticationType.COMPANY)
                ));

        // When
        Slice<BoardListDto.Response> result = roommateBoardService.getBoardList(request, pageable, null);

        // Then
        assertThat(result.hasNext()).isEqualTo(hasNext);
        assertThat(result.isLast()).isEqualTo(!hasNext);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getImageUrl()).isEqualTo("thumbnail.jpg");
            assertThat(response.getTitle()).isEqualTo("룸메이트를 구합니다");
            assertThat(response.getRoomTypes()).containsExactly("원룸");
            assertThat(response.getComeableDate()).isEqualTo(comeableDate);
            assertThat(response.getRegionFullName()).isEqualTo("서울 강남구 역삼동");
            assertThat(response.getMemberId()).isEqualTo(11L);
            assertThat(response.getMemberName()).isEqualTo("작성자");
            assertThat(response.getMemberProfileImageUrl()).isEqualTo("profile.jpg");
            assertThat(response.getMemberAge()).isEqualTo(DateUtils.calculateAge(memberBirth));
            assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(response.isInterested()).isFalse();
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
            assertThat(response.getAuthentications())
                    .containsExactly(AuthenticationType.STUDENT, AuthenticationType.COMPANY);
            assertThat(response.getBadges()).isEmpty();
        });
        verify(roommateBoardFileService).findThumbnailsByBoardIds(List.of(1L));
        verify(authenticationService).findAcceptedByMemberIds(List.of(11L));
    }

    @Test
    @DisplayName("목록 조회는 여러 게시글의 썸네일과 인증 정보를 일괄 조회한다")
    void getBoardListFetchesRelatedDataInBatches() {
        // Given
        BoardListDto.Request request = new BoardListDto.Request();
        request.setKeyword("원룸");
        Long requesterId = 42L;
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime comeableDate = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 25, 9, 0);
        BoardBaseRow firstRow = new BoardBaseRow(
                1L, "첫 게시글", 1_000, 50, 10, comeableDate, 7L,
                "원룸", "역삼동", "강남구", "서울", 11L, "첫 작성자",
                "first-profile.jpg", LocalDate.of(1998, 1, 1), Gender.FEMALE, createdAt
        );
        BoardBaseRow secondRow = new BoardBaseRow(
                2L, "두 번째 게시글", 2_000, 60, 20, comeableDate, 3L,
                "원룸", "서초동", "서초구", "서울", 12L, "두 번째 작성자",
                null, LocalDate.of(1997, 2, 2), Gender.MALE, createdAt.minusHours(1)
        );
        when(roommateBoardRepository.search(
                eq(request), eq(pageable), any(LocalDateTime.class), eq(requesterId)))
                .thenReturn(new SliceImpl<>(List.of(firstRow, secondRow), pageable, false));
        when(roommateBoardFileService.findThumbnailsByBoardIds(List.of(1L, 2L)))
                .thenReturn(List.of(
                        new BoardThumbnailRow(1L, "first.jpg"),
                        new BoardThumbnailRow(2L, "second.jpg")
                ));
        when(authenticationService.findAcceptedByMemberIds(List.of(11L, 12L)))
                .thenReturn(List.of(
                        new MemberAuthenticationRow(11L, AuthenticationType.STUDENT),
                        new MemberAuthenticationRow(12L, AuthenticationType.COMPANY)
                ));
        when(roommateBoardInterestService.findActiveBoardIdsByMemberIdAndBoardIds(
                requesterId, List.of(1L, 2L)))
                .thenReturn(List.of(2L));

        // When
        Slice<BoardListDto.Response> result = roommateBoardService.getBoardList(request, pageable, requesterId);

        // Then
        assertThat(result.getContent())
                .extracting(BoardListDto.Response::getId)
                .containsExactly(1L, 2L);
        assertThat(result.getContent())
                .extracting(BoardListDto.Response::isInterested)
                .containsExactly(false, true);
        verify(roommateBoardFileService).findThumbnailsByBoardIds(List.of(1L, 2L));
        verify(authenticationService).findAcceptedByMemberIds(List.of(11L, 12L));
        verify(roommateBoardInterestService)
                .findActiveBoardIdsByMemberIdAndBoardIds(requesterId, List.of(1L, 2L));
    }

    @Test
    @DisplayName("목록 조회는 활성 관심 수를 기준으로 HOT 뱃지를 조합한다")
    void getBoardListComposesHotBadge() {
        // Given
        BoardListDto.Request request = new BoardListDto.Request();
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        BoardBaseRow baseRow = new BoardBaseRow(
                1L, "인기 신규 게시글", 1_000, 50, 10, LocalDateTime.now().plusDays(1), 100L,
                "원룸", "역삼동", "강남구", "서울", 11L, "작성자",
                null, null, Gender.FEMALE, createdAt
        );
        when(roommateBoardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(null)))
                .thenReturn(new SliceImpl<>(List.of(baseRow), pageable, false));
        when(roommateBoardInterestService.findActiveInterestCountsByBoardIds(List.of(1L)))
                .thenReturn(List.of(new BoardInterestCountRow(1L, 10L)));

        // When
        Slice<BoardListDto.Response> result = roommateBoardService.getBoardList(request, pageable, null);

        // Then
        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getBadges())
                    .containsExactly(RoommateBoardBadgeType.HOT);
            assertThat(response.getMemberAge()).isNull();
        });
    }

    @Test
    @DisplayName("비로그인 사용자의 관심 게시글 필터 요청은 인증 예외를 던진다")
    void getBoardListRejectsAnonymousLikedOnlyRequest() {
        BoardListDto.Request request = new BoardListDto.Request();
        request.setLikedOnly(true);

        assertThatThrownBy(() -> roommateBoardService.getBoardList(
                request,
                PageRequest.of(0, 20),
                null
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED)
        );

        verifyNoInteractions(roommateBoardRepository);
    }

    @Test
    @DisplayName("상세 조회는 조회수 증가 후 원천 데이터를 응답 DTO로 조립한다")
    void getBoardDetailIncreasesHitsThenComposesResponse() {
        // Given
        Long boardId = 1L;
        Long ownerId = 7L;
        Long viewerId = 42L;
        LocalDate birth = LocalDate.of(1998, 1, 1);
        BasicInfoRow basicInfoRow = createBasicInfoRow(boardId, ownerId, birth);
        List<BoardDetailDto.Response.FileDetailDto> images = List.of(
                new BoardDetailDto.Response.FileDetailDto(101L, "thumbnail.jpg"),
                new BoardDetailDto.Response.FileDetailDto(102L, "room.jpg")
        );
        List<RoomExtraOptionInfo> roomExtraOptions = List.of(
                new RoomExtraOptionInfo(201L, "풀옵션", "full-option.png"),
                new RoomExtraOptionInfo(202L, "주차 가능", "parking.png")
        );
        MatchingLifestyleRow visitor = new MatchingLifestyleRow(
                ownerId, 2L, 201L, 2001L, "방문객", "가끔", "가끔 방문해요",
                LifePatternType.SINGLE_CHOICE, "visitor.png");
        MatchingLifestyleRow sleep = new MatchingLifestyleRow(
                ownerId, 1L, 202L, 2002L, "취침", "23:00", "일찍 자요",
                LifePatternType.SCALE, "sleep.png");
        MatchingPreferenceConditionRow condition = new MatchingPreferenceConditionRow(
                ownerId, 3L, 203L, 2003L, "흡연", "비흡연", "비흡연 선호",
                LifePatternType.BOOLEAN, "smoking.png");
        MatchingPreferenceConditionWeightRow conditionWeight = new MatchingPreferenceConditionWeightRow(
                ownerId, 4L, 202L, "청결", "cleaning.png");
        List<AuthenticationType> authenticationTypes = List.of(AuthenticationType.STUDENT);

        when(roommateBoardRepository.increaseHitsById(boardId)).thenReturn(1);
        when(roommateBoardRepository.getBasicInfo(boardId)).thenReturn(Optional.of(basicInfoRow));
        when(roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId)).thenReturn(images);
        when(roommateBoardOptionRepository.getExtraOptionsByBoardId(boardId)).thenReturn(roomExtraOptions);
        when(memberLifePatternService.findMatchingRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of(visitor, sleep));
        when(preferenceConditionService.findRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of(condition));
        when(preferenceConditionService.findWeightRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of(conditionWeight));
        when(roommateScoreService.calculateScore(any(), any()))
                .thenReturn(new Compatibility(76, List.of()));
        when(authenticationService.findTypesByMemberId(ownerId)).thenReturn(authenticationTypes);
        when(roommateBoardInterestRepository.existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, viewerId))
                .thenReturn(true);
        when(roommateBoardInterestService.findActiveInterestCountsByBoardIds(List.of(boardId)))
                .thenReturn(List.of(new BoardInterestCountRow(boardId, 10L)));

        // When
        BoardDetailDto.Response response = roommateBoardService.getBoardDetail(boardId, viewerId);

        // Then
        assertThat(response.getBoardId()).isEqualTo(boardId);
        assertThat(response.getImages()).isSameAs(images);
        assertThat(response.getTitle()).isEqualTo("상세 게시글");
        assertThat(response.getDeposit()).isEqualTo(1_000);
        assertThat(response.getManagementCost()).isEqualTo(10);
        assertThat(response.getMonthlyRent()).isEqualTo(50);
        assertThat(response.getRoomTypeName()).isEqualTo("원룸");
        assertThat(response.getRegionFullName()).isEqualTo("서울 강남구 역삼동");
        assertThat(response.getComeableDateNegotiable()).isTrue();
        assertThat(response.getComeableDate()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(response.getHits()).isEqualTo(3L);
        assertThat(response.getContents()).isEqualTo("상세 내용");
        assertThat(response.getRoomExtraOptions()).isSameAs(roomExtraOptions);
        assertThat(response.getLifeStyles()).extracting(BoardDetailDto.Response.Lifestyle::getName)
                .containsExactly("방문객", "취침");
        assertThat(response.getLifeStyles()).extracting(BoardDetailDto.Response.Lifestyle::getImageUrl)
                .containsExactly("visitor.png", "sleep.png");
        assertThat(response.getConditions()).extracting(BoardDetailDto.Response.Condition::getName)
                .containsExactly("흡연");
        assertThat(response.getConditions()).extracting(BoardDetailDto.Response.Condition::getImageUrl)
                .containsExactly("smoking.png");
        assertThat(response.getConditionWeights()).extracting(BoardDetailDto.Response.ConditionWeight::getName)
                .containsExactly("청결");
        assertThat(response.getConditionWeights()).extracting(BoardDetailDto.Response.ConditionWeight::getImageUrl)
                .containsExactly("cleaning.png");
        assertThat(response.getMemberId()).isEqualTo(ownerId);
        assertThat(response.getMemberName()).isEqualTo("상세작성자");
        assertThat(response.getMemberProfileImageUrl()).isEqualTo("profile.jpg");
        assertThat(response.getMemberAge()).isEqualTo(Period.between(birth, LocalDate.now()).getYears());
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getAuthentications()).isSameAs(authenticationTypes);
        assertThat(response.getCompatibility().getTotalScore()).isEqualTo(76);
        assertThat(response.isInterested()).isTrue();
        assertThat(response.isMine()).isFalse();
        assertThat(response.getBadges()).containsExactly(RoommateBoardBadgeType.HOT);
        InOrder inOrder = inOrder(roommateBoardRepository);
        inOrder.verify(roommateBoardRepository).increaseHitsById(boardId);
        inOrder.verify(roommateBoardRepository).getBasicInfo(boardId);
        verify(roommateBoardFileRepository).getFileDetailDtoByBoardId(boardId);
        verify(roommateBoardOptionRepository).getExtraOptionsByBoardId(boardId);
        verify(memberLifePatternService).findMatchingRowByMemberIdsIn(List.of(ownerId));
        verify(preferenceConditionService).findRowByMemberIdsIn(List.of(ownerId));
        verify(preferenceConditionService).findWeightRowByMemberIdsIn(List.of(ownerId));
        verify(authenticationService).findTypesByMemberId(ownerId);
        verify(roommateBoardInterestRepository).existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, viewerId);
        verify(roommateBoardInterestService).findActiveInterestCountsByBoardIds(List.of(boardId));
    }

    @Test
    @DisplayName("상세 조회는 생활패턴과 중요 조건이 없으면 빈 목록을 응답한다")
    void getBoardDetailReturnsEmptyListsWhenMemberHasNoLifeStylesAndConditionWeights() {
        // Given
        Long boardId = 1L;
        Long ownerId = 7L;
        Long viewerId = 42L;
        BasicInfoRow basicInfoRow = createBasicInfoRow(boardId, ownerId, LocalDate.of(1998, 1, 1));
        when(roommateBoardRepository.increaseHitsById(boardId)).thenReturn(1);
        when(roommateBoardRepository.getBasicInfo(boardId)).thenReturn(Optional.of(basicInfoRow));
        when(roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId)).thenReturn(List.of());
        when(roommateBoardOptionRepository.getExtraOptionsByBoardId(boardId)).thenReturn(List.of());
        when(memberLifePatternService.findMatchingRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(preferenceConditionService.findRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(preferenceConditionService.findWeightRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(authenticationService.findTypesByMemberId(ownerId)).thenReturn(List.of());
        when(roommateBoardInterestRepository.existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, viewerId))
                .thenReturn(false);
        when(roommateBoardInterestService.findActiveInterestCountsByBoardIds(List.of(boardId)))
                .thenReturn(List.of());

        // When
        BoardDetailDto.Response response = roommateBoardService.getBoardDetail(boardId, viewerId);

        // Then
        assertThat(response.getLifeStyles()).isEmpty();
        assertThat(response.getConditionWeights()).isEmpty();
        assertThat(response.isInterested()).isFalse();
        assertThat(response.getBadges()).isEmpty();
        verify(roommateBoardInterestRepository).existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, viewerId);
        verify(roommateBoardInterestService).findActiveInterestCountsByBoardIds(List.of(boardId));
    }

    @Test
    @DisplayName("상세 조회는 로그인 회원이 관심을 누르지 않은 게시글이면 관심 여부를 false로 응답한다")
    void getBoardDetailReturnsFalseWhenViewerIsNotInterested() {
        // Given
        Long boardId = 1L;
        Long ownerId = 7L;
        Long viewerId = 42L;
        BasicInfoRow basicInfoRow = createBasicInfoRow(boardId, ownerId, LocalDate.of(1998, 1, 1));
        when(roommateBoardRepository.increaseHitsById(boardId)).thenReturn(1);
        when(roommateBoardRepository.getBasicInfo(boardId)).thenReturn(Optional.of(basicInfoRow));
        when(roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId)).thenReturn(List.of());
        when(roommateBoardOptionRepository.getExtraOptionsByBoardId(boardId)).thenReturn(List.of());
        when(memberLifePatternService.findMatchingRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(preferenceConditionService.findRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(preferenceConditionService.findWeightRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(authenticationService.findTypesByMemberId(ownerId)).thenReturn(List.of());
        when(roommateBoardInterestRepository.existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, viewerId))
                .thenReturn(false);

        // When
        BoardDetailDto.Response response = roommateBoardService.getBoardDetail(boardId, viewerId);

        // Then
        assertThat(response.isInterested()).isFalse();
        verify(roommateBoardInterestRepository).existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, viewerId);
    }

    @Test
    @DisplayName("상세 조회는 작성자가 자신의 게시글을 조회하면 본인 여부를 true로 응답한다")
    void getBoardDetailReturnsTrueWhenViewerIsOwner() {
        // Given
        Long boardId = 1L;
        Long ownerId = 7L;
        BasicInfoRow basicInfoRow = createBasicInfoRow(boardId, ownerId, LocalDate.of(1998, 1, 1));
        when(roommateBoardRepository.increaseHitsById(boardId)).thenReturn(1);
        when(roommateBoardRepository.getBasicInfo(boardId)).thenReturn(Optional.of(basicInfoRow));
        when(roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId)).thenReturn(List.of());
        when(roommateBoardOptionRepository.getExtraOptionsByBoardId(boardId)).thenReturn(List.of());
        when(memberLifePatternService.findMatchingRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(preferenceConditionService.findRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(preferenceConditionService.findWeightRowByMemberIdsIn(List.of(ownerId))).thenReturn(List.of());
        when(authenticationService.findTypesByMemberId(ownerId)).thenReturn(List.of());
        when(roommateBoardInterestRepository.existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, ownerId))
                .thenReturn(false);

        // When
        BoardDetailDto.Response response = roommateBoardService.getBoardDetail(boardId, ownerId);

        // Then
        assertThat(response.isMine()).isTrue();
        verify(roommateBoardInterestRepository)
                .existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, ownerId);
    }

    @Test
    @DisplayName("상세 조회는 조회수 증가 대상이 없으면 게시글 없음 예외를 던진다")
    void getBoardDetailThrowsWhenHitUpdateDoesNotAffectBoard() {
        // Given
        Long boardId = 999L;
        Long viewerId = 42L;
        when(roommateBoardRepository.increaseHitsById(boardId)).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.getBoardDetail(boardId, viewerId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verify(roommateBoardRepository).increaseHitsById(boardId);
        verify(roommateBoardRepository, never()).getBasicInfo(any());
        verifyNoInteractions(roommateBoardFileRepository, roommateBoardOptionRepository,
                memberLifePatternService, preferenceConditionService,
                authenticationService);
    }

    @Test
    @DisplayName("상세 조회는 조회수 증가 후 기본 정보가 없으면 게시글 없음 예외를 던진다")
    void getBoardDetailThrowsWhenBasicInfoDoesNotExistAfterHitUpdate() {
        // Given
        Long boardId = 999L;
        Long viewerId = 42L;
        when(roommateBoardRepository.increaseHitsById(boardId)).thenReturn(1);
        when(roommateBoardRepository.getBasicInfo(boardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.getBoardDetail(boardId, viewerId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verify(roommateBoardRepository).increaseHitsById(boardId);
        verify(roommateBoardRepository).getBasicInfo(boardId);
        verifyNoInteractions(roommateBoardFileRepository, roommateBoardOptionRepository,
                memberLifePatternService, preferenceConditionService,
                authenticationService);
    }

    @Test
    @DisplayName("수정 폼 조회는 게시글 원천 데이터와 회원 설정 정보를 응답 DTO로 조립한다")
    void getEditFormComposesResponseFromBoardAndMemberSettings() {
        // Given
        Long boardId = 1L;
        Long memberId = 7L;
        EditFormRow editFormRow = createEditFormRow();
        List<BoardDetailDto.Response.FileDetailDto> images = List.of(
                new BoardDetailDto.Response.FileDetailDto(101L, "thumbnail.jpg"),
                new BoardDetailDto.Response.FileDetailDto(102L, "room.jpg")
        );
        List<RoomExtraOptionInfo> roomExtraOptions = List.of(
                new RoomExtraOptionInfo(201L, "풀옵션", "full-option.png"),
                new RoomExtraOptionInfo(202L, "주차 가능", "parking.png")
        );
        BoardDetailDto.Response.Lifestyle lifestyle = new BoardDetailDto.Response.Lifestyle(
                1L, "취침", "23:00", "일찍 자요", LifePatternType.SCALE, "sleep.png");
        BoardDetailDto.Response.Condition condition = new BoardDetailDto.Response.Condition(
                2L, "흡연", "비흡연", "비흡연 선호", LifePatternType.BOOLEAN, "smoking.png");
        BoardDetailDto.Response.ConditionWeight conditionWeight = new BoardDetailDto.Response.ConditionWeight(
                3L, "청결", "cleaning.png");

        when(roommateBoardRepository.getEditRow(boardId)).thenReturn(Optional.of(editFormRow));
        when(roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId)).thenReturn(images);
        when(roommateBoardOptionRepository.getExtraOptionsByBoardId(boardId)).thenReturn(roomExtraOptions);
        when(memberLifePatternService.findLifeStyleDtoByMemberId(memberId)).thenReturn(List.of(lifestyle));
        when(preferenceConditionService.findAllConditionByMemberId(memberId)).thenReturn(List.of(condition));
        when(preferenceConditionService.findAllConditionWeightByMemberId(memberId))
                .thenReturn(List.of(conditionWeight));

        // When
        BoardEditDto.Response response = roommateBoardService.getEditForm(memberId, boardId);

        // Then
        assertThat(response.getImages()).isSameAs(images);
        assertThat(response.getTitle()).isEqualTo("수정 게시글");
        assertThat(response.getDeposit()).isEqualTo(1_000);
        assertThat(response.getMonthlyRent()).isEqualTo(50);
        assertThat(response.getManagementCost()).isEqualTo(10);
        assertThat(response.getRoomType().getRoomTypeId()).isEqualTo(11L);
        assertThat(response.getRoomType().getName()).isEqualTo("원룸");
        assertThat(response.getRoomType().getImageUrl()).isEqualTo("one-room.png");
        assertThat(response.getRegion().getRegionId()).isEqualTo(33L);
        assertThat(response.getRegion().getFullName()).isEqualTo("서울 강남구 역삼동");
        assertThat(response.getComeableDateNegotiable()).isTrue();
        assertThat(response.getComeableDate()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(response.getRoomExtraOptions()).isSameAs(roomExtraOptions);
        assertThat(response.getContents()).isEqualTo("수정 내용");
        assertThat(response.getLifeStyles()).containsExactly(lifestyle);
        assertThat(response.getConditions()).containsExactly(condition);
        assertThat(response.getConditionWeights()).containsExactly(conditionWeight);
        verify(memberLifePatternService).findLifeStyleDtoByMemberId(memberId);
        verify(preferenceConditionService).findAllConditionByMemberId(memberId);
        verify(preferenceConditionService).findAllConditionWeightByMemberId(memberId);
    }

    @Test
    @DisplayName("수정 폼 조회는 대상 게시글이 없으면 게시글 없음 예외를 던지고 부가 정보를 조회하지 않는다")
    void getEditFormThrowsWhenBoardDoesNotExist() {
        // Given
        Long boardId = 999L;
        Long memberId = 7L;
        when(roommateBoardRepository.getEditRow(boardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.getEditForm(memberId, boardId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verify(roommateBoardRepository).getEditRow(boardId);
        verifyNoInteractions(roommateBoardFileRepository, roommateBoardOptionRepository,
                memberLifePatternService, preferenceConditionService);
    }

    @Test
    @DisplayName("게시글 수정은 기본 정보와 옵션 및 이미지 최종 상태를 반영한다")
    void modifyUpdatesBoardOptionsAndImageState() throws IOException {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType newRoomType = org.mockito.Mockito.mock(RoomType.class);
        Region newRegion = org.mockito.Mockito.mock(Region.class);
        BoardModifyDto.Request request = createModifyRequest();
        request.setDeleteExtraOptionIds(List.of(10L));
        request.setNewExtraOptionIds(List.of(20L, 21L));

        RoomExtraOption deleteExtraOption = mockExtraOption(10L);
        RoomExtraOption keepExtraOption = mockExtraOption(11L);
        RoomExtraOption newExtraOption = org.mockito.Mockito.mock(RoomExtraOption.class);
        RoomExtraOption anotherNewExtraOption = org.mockito.Mockito.mock(RoomExtraOption.class);
        RoommateBoardOption deleteBoardOption = RoommateBoardOption.builder()
                .roommateBoard(roommateBoard)
                .roomExtraOption(deleteExtraOption)
                .build();
        RoommateBoardOption keepBoardOption = RoommateBoardOption.builder()
                .roommateBoard(roommateBoard)
                .roomExtraOption(keepExtraOption)
                .build();

        RoommateBoardFile keptImage = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(createFile("keep.jpg", "saved-keep.jpg", "jpg"))
                .isThumbnail(true)
                .build();
        RoommateBoardFile thumbnailImage = RoommateBoardFile.builder()
                .id(102L)
                .roommateBoard(roommateBoard)
                .file(createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg"))
                .isThumbnail(false)
                .build();
        File deletedFile = createFile("delete.jpg", "saved-delete.jpg", "jpg");
        RoommateBoardFile deletedImage = RoommateBoardFile.builder()
                .id(103L)
                .roommateBoard(roommateBoard)
                .file(deletedFile)
                .isThumbnail(false)
                .build();
        List<RoommateBoardFile> persistedBoardFiles = new ArrayList<>(
                List.of(keptImage, thumbnailImage, deletedImage));

        MultipartFile newMultipartFile = emptyMultipartFile();
        File newFile = createFile("new.jpg", "saved-new.jpg", "jpg");
        request.setExistingImages(List.of(
                createExistingFileDto(101L, false),
                createExistingFileDto(102L, true)));
        request.setNewImages(List.of(createNewFileDto(0, false)));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(newRoomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(newRegion));
        when(roommateBoardOptionRepository.findWithRoomExtraOptionByBoardId(boardId))
                .thenReturn(List.of(deleteBoardOption, keepBoardOption));
        when(roomExtraOptionService.findAllById(List.of(20L, 21L)))
                .thenReturn(List.of(newExtraOption, anotherNewExtraOption));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard))
                .thenAnswer(invocation -> new ArrayList<>(persistedBoardFiles));
        org.mockito.Mockito.doAnswer(invocation -> {
            persistedBoardFiles.remove(invocation.getArgument(0));
            return null;
        }).when(roommateBoardFileRepository).delete(any(RoommateBoardFile.class));
        when(fileService.save(newMultipartFile, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(newFile);
        when(roommateBoardFileRepository.save(any(RoommateBoardFile.class))).thenAnswer(invocation -> {
            RoommateBoardFile boardFile = invocation.getArgument(0);
            persistedBoardFiles.add(boardFile);
            return boardFile;
        });

        // When
        BoardModifyDto.Response response = roommateBoardService.modify(7L, boardId, request, List.of(newMultipartFile));

        // Then
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(roommateBoard.getTitle()).isEqualTo("수정 제목");
        assertThat(roommateBoard.getContents()).isEqualTo("수정 내용");
        assertThat(roommateBoard.getDeposit()).isEqualTo(2_000);
        assertThat(roommateBoard.getMonthlyRent()).isEqualTo(70);
        assertThat(roommateBoard.getManagementCost()).isEqualTo(15);
        assertThat(roommateBoard.getComeableDateNegotiable()).isEqualTo(request.getComeableDateNegotiable());
        assertThat(roommateBoard.getComeableDate()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 0));
        assertThat(roommateBoard.getRoomType()).isSameAs(newRoomType);
        assertThat(roommateBoard.getRegion()).isSameAs(newRegion);

        verify(roommateBoardOptionRepository).deleteAll(boardOptionsCaptor.capture());
        assertThat(toList(boardOptionsCaptor.getValue())).containsExactly(deleteBoardOption);
        verify(roommateBoardOptionRepository).saveAll(boardOptionsCaptor.capture());
        assertThat(toList(boardOptionsCaptor.getValue()))
                .extracting(RoommateBoardOption::getRoomExtraOption)
                .containsExactly(newExtraOption, anotherNewExtraOption);

        assertThat(keptImage.getIsThumbnail()).isFalse();
        assertThat(thumbnailImage.getIsThumbnail()).isTrue();
        assertThat(deletedFile.getIsDeleted()).isTrue();
        verify(roommateBoardFileRepository).delete(deletedImage);
        verify(fileService).save(newMultipartFile, FileType.ROOMMATE_BOARD_IMAGE);
        verify(roommateBoardFileRepository).save(boardFileCaptor.capture());
        RoommateBoardFile newBoardFile = boardFileCaptor.getValue();
        assertThat(newBoardFile.getRoommateBoard()).isSameAs(roommateBoard);
        assertThat(newBoardFile.getFile()).isSameAs(newFile);
        assertThat(newBoardFile.getIsThumbnail()).isFalse();
        assertThat(persistedBoardFiles).containsExactly(keptImage, thumbnailImage, newBoardFile);
    }

    @Test
    @DisplayName("입주 시기 협의 가능으로 수정하면 입주 가능일을 비울 수 있다")
    void modifyAllowsNegotiableComeableDateWithoutDate() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoardFile existingThumbnail = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg"))
                .isThumbnail(true)
                .build();
        BoardModifyDto.Request request = createModifyRequest();
        request.setComeableDateNegotiable(true);
        request.setComeableDate(null);
        request.setExistingImages(List.of(createExistingFileDto(101L, true)));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard)).thenReturn(List.of(existingThumbnail));

        // When
        BoardModifyDto.Response response = roommateBoardService.modify(7L, boardId, request, null);

        // Then
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(roommateBoard.getComeableDateNegotiable()).isTrue();
        assertThat(roommateBoard.getComeableDate()).isNull();
    }

    @Test
    @DisplayName("게시글 수정 후 이미지가 10장을 초과하면 파일 개수 초과 예외를 던진다")
    void modifyThrowsWhenBoardImageCountExceedsLimit() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        List<RoommateBoardFile> boardFiles = new ArrayList<>();
        List<ExistingFileDto> existingImages = new ArrayList<>();
        for (long id = 1; id <= 11; id++) {
            boardFiles.add(RoommateBoardFile.builder()
                    .id(id)
                    .roommateBoard(roommateBoard)
                    .file(createFile("room-" + id + ".jpg", "saved-room-" + id + ".jpg", "jpg"))
                    .isThumbnail(id == 1)
                    .build());
            existingImages.add(createExistingFileDto(id, id == 1));
        }
        BoardModifyDto.Request request = createModifyRequest();
        request.setExistingImages(existingImages);

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard)).thenReturn(boardFiles);

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(7L, boardId, request, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_EXCEEDED));
    }

    @Test
    @DisplayName("게시글 수정 후 이미지가 없으면 썸네일 없이도 수정에 성공한다")
    void modifyAllowsEmptyImageResultWithoutThumbnail() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        File deletedFile = createFile("delete.jpg", "saved-delete.jpg", "jpg");
        RoommateBoardFile deletedImage = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(deletedFile)
                .isThumbnail(true)
                .build();
        List<RoommateBoardFile> persistedBoardFiles = new ArrayList<>(List.of(deletedImage));
        BoardModifyDto.Request request = createModifyRequest();
        request.setExistingImages(List.of());
        request.setNewImages(List.of());

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard))
                .thenAnswer(invocation -> new ArrayList<>(persistedBoardFiles));
        org.mockito.Mockito.doAnswer(invocation -> {
            persistedBoardFiles.remove(invocation.getArgument(0));
            return null;
        }).when(roommateBoardFileRepository).delete(any(RoommateBoardFile.class));

        // When
        BoardModifyDto.Response response = roommateBoardService.modify(7L, boardId, request, null);

        // Then
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(deletedFile.getIsDeleted()).isTrue();
        assertThat(persistedBoardFiles).isEmpty();
        verify(roommateBoardFileRepository).delete(deletedImage);
        verify(roomExtraOptionService, never()).findAllById(any());
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    @DisplayName("게시글 수정 후 썸네일이 정확히 1장이 아니면 썸네일 개수 예외를 던진다")
    void modifyThrowsWhenThumbnailCountIsNotOne() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoardFile firstImage = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(createFile("first.jpg", "saved-first.jpg", "jpg"))
                .isThumbnail(true)
                .build();
        RoommateBoardFile secondImage = RoommateBoardFile.builder()
                .id(102L)
                .roommateBoard(roommateBoard)
                .file(createFile("second.jpg", "saved-second.jpg", "jpg"))
                .isThumbnail(false)
                .build();
        BoardModifyDto.Request request = createModifyRequest();
        request.setExistingImages(List.of(
                createExistingFileDto(101L, false),
                createExistingFileDto(102L, false)));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard))
                .thenReturn(List.of(firstImage, secondImage));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(7L, boardId, request, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_THUMBNAIL_EXCEEDED));
    }

    @Test
    @DisplayName("다른 회원의 게시글을 수정하면 권한 없음 예외를 던지고 수정 데이터를 조회하지 않는다")
    void modifyThrowsWhenRequesterIsNotBoardOwner() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard(7L);
        BoardModifyDto.Request request = createModifyRequest();
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(999L, boardId, request, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_FORBIDDEN));
        verifyNoInteractions(metaService, roommateBoardOptionRepository, roommateBoardFileRepository,
                fileService, fileRepository);
    }

    @Test
    @DisplayName("신규 추가 옵션 목록이 null이면 옵션 조회 없이 게시글 수정에 성공한다")
    void modifyAllowsNullNewExtraOptionIdsWithoutOptionLookup() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoardFile existingThumbnail = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg"))
                .isThumbnail(true)
                .build();
        BoardModifyDto.Request request = createModifyRequest();
        request.setNewExtraOptionIds(null);
        request.setExistingImages(List.of(createExistingFileDto(101L, true)));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard)).thenReturn(List.of(existingThumbnail));

        // When
        BoardModifyDto.Response response = roommateBoardService.modify(7L, boardId, request, null);

        // Then
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(roomExtraOptionService, never()).findAllById(any());
        verify(roommateBoardOptionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 신규 추가 옵션이 포함되면 추가 옵션 없음 예외를 던진다")
    void modifyThrowsWhenNewExtraOptionDoesNotExist() {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        BoardModifyDto.Request request = createModifyRequest();
        request.setNewExtraOptionIds(List.of(20L, 21L));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roomExtraOptionService.findAllById(List.of(20L, 21L)))
                .thenReturn(List.of(org.mockito.Mockito.mock(RoomExtraOption.class)));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(7L, boardId, request, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MetaErrorCode.EXTRA_OPTION_NOT_FOUND));
        verifyNoInteractions(roommateBoardFileRepository, fileService, fileRepository);
    }

    @Test
    @DisplayName("게시글 수정 중 신규 이미지 업로드가 실패하면 업로드 실패 예외를 던지고 신규 파일 연결을 저장하지 않는다")
    void modifyThrowsWhenNewImageUploadFails() throws IOException {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoardFile existingThumbnail = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg"))
                .isThumbnail(true)
                .build();
        MultipartFile failedFile = emptyMultipartFile();
        BoardModifyDto.Request request = createModifyRequest();
        request.setExistingImages(List.of(createExistingFileDto(101L, true)));
        request.setNewImages(List.of(createNewFileDto(0, false)));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard)).thenReturn(List.of(existingThumbnail));
        when(fileService.save(failedFile, FileType.ROOMMATE_BOARD_IMAGE))
                .thenThrow(new IOException("upload failed"));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(7L, boardId, request, List.of(failedFile)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_FAILED));
        verify(fileService, never()).deleteAll(any());
        verify(fileRepository, never()).save(any(File.class));
        verify(roommateBoardFileRepository, never()).save(any(RoommateBoardFile.class));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 수정하면 게시글 없음 예외를 던지고 부가 정보를 조회하지 않는다")
    void modifyThrowsWhenBoardDoesNotExist() {
        // Given
        Long boardId = 999L;
        BoardModifyDto.Request request = createModifyRequest();
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(7L, boardId, request, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verifyNoInteractions(metaService, fileService, fileRepository);
    }

    @Test
    @DisplayName("게시글 수정 중 신규 이미지 메타데이터가 파일 파트와 매칭되지 않으면 잘못된 요청 예외를 던진다")
    void modifyThrowsWhenNewImageFileIndexDoesNotMatchFilesPart() throws IOException {
        // Given
        Long boardId = 1L;
        RoommateBoard roommateBoard = createRoommateBoard();
        RoomType roomType = org.mockito.Mockito.mock(RoomType.class);
        Region region = org.mockito.Mockito.mock(Region.class);
        RoommateBoardFile existingThumbnail = RoommateBoardFile.builder()
                .id(101L)
                .roommateBoard(roommateBoard)
                .file(createFile("thumbnail.jpg", "saved-thumbnail.jpg", "jpg"))
                .isThumbnail(true)
                .build();
        BoardModifyDto.Request request = createModifyRequest();
        request.setExistingImages(List.of(createExistingFileDto(101L, true)));
        request.setNewImages(List.of(createNewFileDto(1, false)));

        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(metaService.findByRoomTypeId(11L)).thenReturn(roomType);
        when(metaService.findByRegionId(22L)).thenReturn(Optional.of(region));
        when(roommateBoardFileRepository.findByRoommateBoard(roommateBoard)).thenReturn(List.of(existingThumbnail));

        // When & Then
        assertThatThrownBy(() -> roommateBoardService.modify(7L, boardId, request, List.of(emptyMultipartFile())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        verify(fileService, never()).save(any(MultipartFile.class), any(FileType.class));
        verifyNoInteractions(fileRepository);
    }


    private BoardDto.Request createRequest(FileDto... images) {
        BoardDto.Request request = new BoardDto.Request();
        request.setTitle("Looking for a roommate");
        request.setContents("Quiet home near the station");
        request.setDeposit(10_000);
        request.setMountlyRent(500);
        request.setManagementCost(50);
        request.setRoomTypeId(1L);
        request.setRegionId(2L);
        request.setComeableDateNegotiable(false);
        request.setComeableDate(LocalDateTime.of(2026, 7, 1, 9, 0));
        request.setImages(List.of(images));
        return request;
    }

    private BoardModifyDto.Request createModifyRequest() {
        BoardModifyDto.Request request = new BoardModifyDto.Request();
        request.setTitle("수정 제목");
        request.setContents("수정 내용");
        request.setDeposit(2_000);
        request.setMonthlyRent(70);
        request.setManagementCost(15);
        request.setRoomTypeId(11L);
        request.setRegionId(22L);
        request.setComeableDateNegotiable(false);
        request.setComeableDate(LocalDateTime.of(2026, 8, 1, 10, 0));
        request.setDeleteExtraOptionIds(List.of());
        request.setNewExtraOptionIds(List.of());
        request.setExistingImages(List.of());
        request.setNewImages(List.of());
        return request;
    }

    private FileDto createFileDto(Integer fileIndex, boolean thumbnail) {
        FileDto fileDto = new FileDto();
        fileDto.setFileIndex(fileIndex);
        fileDto.setThumbnail(thumbnail);
        return fileDto;
    }

    private ExistingFileDto createExistingFileDto(Long boardFileId, boolean thumbnail) {
        ExistingFileDto existingFileDto = new ExistingFileDto();
        existingFileDto.setBoardFileId(boardFileId);
        existingFileDto.setThumbnail(thumbnail);
        return existingFileDto;
    }

    private NewFileDto createNewFileDto(Integer fileIndex, boolean thumbnail) {
        NewFileDto newFileDto = new NewFileDto();
        newFileDto.setFileIndex(fileIndex);
        newFileDto.setThumbnail(thumbnail);
        return newFileDto;
    }

    private ReportDto.Request createReportRequest(String contents) {
        ReportDto.Request request = new ReportDto.Request();
        request.setContents(contents);
        return request;
    }

    private RoommateBoard createRoommateBoard() {
        return createRoommateBoard(7L);
    }

    private RoommateBoard createRoommateBoard(Long memberId) {
        return RoommateBoard.builder()
                .member(Member.builder().id(memberId).build())
                .title("기존 제목")
                .contents("기존 내용")
                .deposit(1_000)
                .monthlyRent(50)
                .managementCost(10)
                .roomType(org.mockito.Mockito.mock(RoomType.class))
                .region(org.mockito.Mockito.mock(Region.class))
                .comeableDateNegotiable(false)
                .comeableDate(LocalDateTime.of(2026, 7, 1, 9, 0))
                .build();
    }

    private RoomExtraOption mockExtraOption(Long id) {
        RoomExtraOption extraOption = org.mockito.Mockito.mock(RoomExtraOption.class);
        when(extraOption.getId()).thenReturn(id);
        return extraOption;
    }

    private File createFile(String originalFileName, String savedFileName, String extension) {
        return File.builder()
                .type(FileType.ROOMMATE_BOARD_IMAGE)
                .originalFileName(originalFileName)
                .savedFileName(savedFileName)
                .fileExt(extension)
                .build();
    }

    private BasicInfoRow createBasicInfoRow(Long boardId, Long memberId, LocalDate birth) {
        return new BasicInfoRow(
                boardId,
                "상세 게시글",
                1_000,
                10,
                50,
                "원룸",
                "역삼동",
                "강남구",
                "서울",
                true,
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 12, 0),
                3L,
                "상세 내용",
                memberId,
                "상세작성자",
                "profile.jpg",
                birth,
                Gender.FEMALE
        );
    }

    private EditFormRow createEditFormRow() {
        return new EditFormRow(
                "수정 게시글",
                1_000,
                50,
                10,
                11L,
                "원룸",
                "one-room.png",
                33L,
                "역삼동",
                "강남구",
                "서울",
                true,
                LocalDateTime.of(2026, 7, 1, 9, 0),
                "수정 내용"
        );
    }

    private MultipartFile emptyMultipartFile() {
        return org.mockito.Mockito.mock(MultipartFile.class);
    }

    private <T> List<T> toList(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }

    @Test
    @DisplayName("백오피스 게시글 목록 조회 성공 테스트 (findBackOfficeBoardList)")
    void findBackOfficeBoardListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoBoardListDto.Response.BoardInfo boardInfo = new BoBoardListDto.Response.BoardInfo();
        boardInfo.setId(100L);
        boardInfo.setTitle("백오피스 게시글");
        BoBoardListDto.Request request = new BoBoardListDto.Request();

        when(roommateBoardRepository.findBackOfficeBoardList(pageable, request)).thenReturn(List.of(boardInfo));

        // when
        List<BoBoardListDto.Response.BoardInfo> result = roommateBoardService.findBackOfficeBoardList(pageable, request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getTitle()).isEqualTo("백오피스 게시글");
        verify(roommateBoardRepository).findBackOfficeBoardList(pageable, request);
    }

    @Test
    @DisplayName("백오피스 게시글 상세 조회 성공 테스트 (findBackOffcieBoard)")
    void findBackOfficeBoardDetailSuccessTest() {
        // given
        Long boardId = 100L;
        BoBoardDetailDto.Response detail = new BoBoardDetailDto.Response();
        detail.setTitle("상세 제목");

        when(roommateBoardRepository.findBackOffcieBoard(boardId)).thenReturn(detail);

        // when
        BoBoardDetailDto.Response result = roommateBoardService.findBackOffcieBoard(boardId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("상세 제목");
        verify(roommateBoardRepository).findBackOffcieBoard(boardId);
    }

    @Test
    @DisplayName("백오피스 게시글 삭제 성공 테스트 (deleteBackOfficeBoard)")
    void deleteBackOfficeBoardSuccessTest() {
        // given
        Long boardId = 100L;
        String rejectReason = "삭제 사유";
        RoommateBoard board = org.mockito.Mockito.spy(createRoommateBoard(10L));
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(board));
 
        // when
        RoommateBoard result = roommateBoardService.deleteBackOfficeBoard(boardId, rejectReason);
 
        // then
        assertThat(result).isSameAs(board);
        assertThat(board.getIsDeleted()).isTrue();
        verify(board).softDelete(rejectReason);
        verify(roommateBoardRepository).findById(boardId);
    }
 
    @Test
    @DisplayName("백오피스 게시글 삭제 시 게시글이 없으면 BusinessException 발생")
    void deleteBackOfficeBoardNotFoundTest() {
        // given
        Long boardId = 100L;
        String rejectReason = "삭제 사유";
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> roommateBoardService.deleteBackOfficeBoard(boardId, rejectReason))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND);
    }
}
