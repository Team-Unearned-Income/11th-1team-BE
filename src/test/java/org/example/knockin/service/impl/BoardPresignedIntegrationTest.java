package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.knockin.board.dto.BoardPreSignedDto;
import org.example.knockin.board.dto.BoardPreSignedModifyDto;
import org.example.knockin.board.dto.BoardModifyDto.Request.ExistingFileDto;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.board.repository.RoommateBoardFileRepository;
import org.example.knockin.board.service.impl.RoommateBoardServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardFileServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardOptionServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardInterestServiceImpl;
import org.example.knockin.global.config.JpaConfig;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.global.config.RoommateBoardPolicy;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.meta.repository.FileRepository;
import org.example.knockin.meta.service.FileUploadService;
import org.example.knockin.meta.service.impl.FileServiceImpl;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, RoommateBoardPolicy.class, RoommateBoardServiceImpl.class,
        RoommateBoardFileServiceImpl.class, FileServiceImpl.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BoardPresignedIntegrationTest {
    @Autowired RoommateBoardServiceImpl service;
    @Autowired RoommateBoardRepository boards;
    @Autowired RoommateBoardFileRepository boardFiles;
    @Autowired FileRepository files;
    @Autowired EntityManager em;
    @Autowired RoommateBoardPolicy policy;
    @MockitoBean MemberServiceImpl members;
    @MockitoBean MetaServiceImpl meta;
    @MockitoBean FileUploadService storage;
    @MockitoBean RoommateBoardOptionServiceImpl options;
    @MockitoBean RoommateBoardInterestServiceImpl interests;
    @MockitoBean org.example.knockin.util.service.RoommateScoreService scores;
    @MockitoBean org.example.knockin.life.service.impl.PreferenceConditionServiceImpl preferences;
    @MockitoBean org.example.knockin.life.service.impl.MemberLifePatternService patterns;
    @MockitoBean org.example.knockin.verification.service.impl.AuthenticationServiceImpl authentication;
    @MockitoBean org.example.knockin.meta.service.impl.AlarmServiceImpl alarms;
    @MockitoBean org.example.knockin.meta.service.impl.PushNotificationServiceImpl notifications;
    @MockitoBean org.example.knockin.declaration.service.impl.DeclarationServiceImpl declarations;
    private Member member;
    private RoomType roomType;
    private Region region;

    @BeforeEach
    void setUp() {
        policy.setImageMaxCount(10);
        policy.setThumbnailImageMaxCount(1);
        member = Member.builder().providerType(LoginProviderType.KAKAO).providerId(UUID.randomUUID().toString())
                .role(MemberRole.USER).build();
        roomType = RoomType.builder().name("원룸").build();
        region = Region.builder().name("서초동").scope(3).build();
        em.persist(member); em.persist(roomType); em.persist(region);
        when(members.findById(member.getId())).thenReturn(Optional.of(member));
        when(meta.findByRoomTypeId(roomType.getId())).thenReturn(roomType);
        when(meta.findByRegionId(region.getId())).thenReturn(Optional.of(region));
    }

    @Test
    void persistsBoardAndFileMetadataWithoutReuploadingImageBytes() throws Exception {
        when(storage.getUploadedImageUrl("owned-first.jpg", member.getId())).thenReturn("https://images.example.com/owned-first.jpg");
        var request = request(List.of(new BoardPreSignedDto.Request.FileDto("owned-first.jpg", "room.jpeg", true)));
        var response = service.saveBoardPreSigned(request, member.getId());
        assertThat(response.getUpdatedAt()).isNotNull();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        assertThat(boards.count()).isEqualTo(1);
        assertThat(files.count()).isEqualTo(1);
        assertThat(boardFiles.count()).isEqualTo(1);
        var file = files.findAll().getFirst();
        assertThat(file.getOriginalFileName()).isEqualTo("room.jpeg");
        assertThat(file.getFileExt()).isEqualTo("jpg");
        assertThat(file.getSavedFileName()).isEqualTo("https://images.example.com/owned-first.jpg");
        verify(storage, never()).uploadImage(any());
    }

    @Test
    void secondMissingImageRollsBackBoardAndFirstFileButKeepsR2ObjectsForRetry() throws Exception {
        when(storage.getUploadedImageUrl("owned-first.jpg", member.getId())).thenReturn("https://images.example.com/owned-first.jpg");
        when(storage.getUploadedImageUrl("owned-second.jpg", member.getId())).thenThrow(new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        var request = request(List.of(new BoardPreSignedDto.Request.FileDto("owned-first.jpg", "first.jpg", true),
                new BoardPreSignedDto.Request.FileDto("owned-second.jpg", "second.jpg", false)));
        assertThatThrownBy(() -> service.saveBoardPreSigned(request, member.getId())).isInstanceOf(BusinessException.class);
        TestTransaction.end();
        assertThat(boards.count()).isZero();
        assertThat(files.count()).isZero();
        assertThat(boardFiles.count()).isZero();
        verify(storage, never()).deleteImage(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void commitsBoardWithoutImagesAndDoesNotCallStorage(boolean omitImages) {
        var request = request(omitImages ? null : List.of());
        var response = service.saveBoardPreSigned(request, member.getId());
        assertThat(response.getUpdatedAt()).isNotNull();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        assertThat(boards.count()).isEqualTo(1);
        assertThat(files.count()).isZero();
        assertThat(boardFiles.count()).isZero();
        verifyNoInteractions(storage);
    }


    @Test
    void modifiesBasicInfoRetainsImageRemovesImageAndAddsUploadedImage() throws Exception {
        var board = seedBoardWithImages();
        var initial = boardFiles.findByRoommateBoard(board);
        Long retainedId = initial.getFirst().getId();
        Long removedFileId = initial.getLast().getFile().getId();
        var request = modifyRequest();
        request.setExistingImages(List.of(new ExistingFileDto(retainedId, false)));
        request.setNewImages(List.of(new BoardPreSignedModifyDto.Request.FileDto("new.jpg", "new.jpeg", true)));
        when(storage.getUploadedImageUrl("new.jpg", member.getId())).thenReturn("https://images.example.com/new.jpg");

        var response = service.modifyBoardPreSigned(member.getId(), board.getId(), request);
        assertThat(response.getUpdatedAt()).isNotNull();
        TestTransaction.flagForCommit(); TestTransaction.end();
        assertThat(boards.findById(board.getId()).orElseThrow().getTitle()).isEqualTo("수정 제목");
        assertThat(boardFiles.count()).isEqualTo(2);
        assertThat(boardFiles.findById(retainedId).orElseThrow().getIsThumbnail()).isFalse();
        assertThat(boardFiles.findAll()).filteredOn(RoommateBoardFile::getIsThumbnail).hasSize(1);
        assertThat(files.findById(removedFileId).orElseThrow().getIsDeleted()).isTrue();
        assertThat(files.findAll()).filteredOn(f -> !f.getIsDeleted()).hasSize(2);
        verify(storage).getUploadedImageUrl("new.jpg", member.getId());
        verify(storage, never()).uploadImage(any());
        verify(storage, never()).deleteImage(any());
    }

    @Test
    void thumbnailCanMoveBetweenExistingImagesWithoutStorageCalls() {
        var board = seedBoardWithImages();
        var initial = boardFiles.findByRoommateBoard(board);
        var request = modifyRequest();
        request.setExistingImages(List.of(new ExistingFileDto(initial.getFirst().getId(), false),
                new ExistingFileDto(initial.getLast().getId(), true)));
        service.modifyBoardPreSigned(member.getId(), board.getId(), request);
        TestTransaction.flagForCommit(); TestTransaction.end();
        assertThat(boardFiles.findById(initial.getLast().getId()).orElseThrow().getIsThumbnail()).isTrue();
        assertThat(boardFiles.findById(initial.getFirst().getId()).orElseThrow().getIsThumbnail()).isFalse();
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void removesAllImagesAndAllowsImageFreeBoard(boolean omitImages) {
        var board = seedBoardWithImages();
        var request = modifyRequest();
        if (!omitImages) { request.setExistingImages(List.of()); request.setNewImages(List.of()); }
        service.modifyBoardPreSigned(member.getId(), board.getId(), request);
        TestTransaction.flagForCommit(); TestTransaction.end();
        assertThat(boardFiles.count()).isZero();
        assertThat(files.findAll()).allMatch(File::getIsDeleted);
        assertThat(boards.findById(board.getId()).orElseThrow().getTitle()).isEqualTo("수정 제목");
        verifyNoInteractions(storage);
    }

    @Test
    void missingSecondUploadRollsBackTextThumbnailDeletionAndFirstNewFile() throws Exception {
        var board = seedBoardWithImages();
        var initial = boardFiles.findByRoommateBoard(board);
        var request = modifyRequest();
        request.setExistingImages(List.of(new ExistingFileDto(initial.getFirst().getId(), false)));
        request.setNewImages(List.of(new BoardPreSignedModifyDto.Request.FileDto("first.jpg", "first.jpg", true),
                new BoardPreSignedModifyDto.Request.FileDto("missing.jpg", "missing.jpg", false)));
        when(storage.getUploadedImageUrl("first.jpg", member.getId())).thenReturn("https://images.example.com/first.jpg");
        when(storage.getUploadedImageUrl("missing.jpg", member.getId())).thenThrow(new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        assertThatThrownBy(() -> service.modifyBoardPreSigned(member.getId(), board.getId(), request))
                .isInstanceOf(BusinessException.class);
        TestTransaction.end();
        assertThat(boards.findById(board.getId()).orElseThrow().getTitle()).isEqualTo("원래 제목");
        assertThat(boardFiles.count()).isEqualTo(2);
        assertThat(boardFiles.findById(initial.getFirst().getId()).orElseThrow().getIsThumbnail()).isTrue();
        assertThat(files.count()).isEqualTo(2);
        assertThat(files.findAll()).noneMatch(File::getIsDeleted);
        verify(storage, never()).deleteImage(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign-id", "duplicate-id", "duplicate-key", "reused-key", "no-thumbnail", "two-thumbnails", "too-many"})
    void rejectsInvalidFinalImagesWithoutChangingBoardOrCallingStorage(String scenario) {
        var board = seedBoardWithImages();
        var initial = boardFiles.findByRoommateBoard(board);
        var request = modifyRequest();
        Long id = initial.getFirst().getId();
        request.setExistingImages(List.of(new ExistingFileDto(id, true)));
        switch (scenario) {
            case "foreign-id" -> request.setExistingImages(List.of(new ExistingFileDto(Long.MAX_VALUE, true)));
            case "duplicate-id" -> request.setExistingImages(List.of(new ExistingFileDto(id, true), new ExistingFileDto(id, false)));
            case "duplicate-key" -> request.setNewImages(List.of(new BoardPreSignedModifyDto.Request.FileDto("same.jpg", "a.jpg", false), new BoardPreSignedModifyDto.Request.FileDto("same.jpg", "b.jpg", false)));
            case "reused-key" -> request.setNewImages(List.of(new BoardPreSignedModifyDto.Request.FileDto("uploads/old-1.jpg", "old.jpg", false)));
            case "no-thumbnail" -> request.setExistingImages(List.of(new ExistingFileDto(id, false)));
            case "two-thumbnails" -> request.setNewImages(List.of(new BoardPreSignedModifyDto.Request.FileDto("new.jpg", "new.jpg", true)));
            case "too-many" -> request.setNewImages(java.util.stream.IntStream.range(0, 10).mapToObj(n -> new BoardPreSignedModifyDto.Request.FileDto("new-" + n + ".jpg", "new.jpg", false)).toList());
        }
        assertThatThrownBy(() -> service.modifyBoardPreSigned(member.getId(), board.getId(), request))
                .isInstanceOf(BusinessException.class);
        TestTransaction.end();
        assertThat(boards.findById(board.getId()).orElseThrow().getTitle()).isEqualTo("원래 제목");
        assertThat(boardFiles.count()).isEqualTo(2);
        assertThat(files.findAll()).noneMatch(File::getIsDeleted);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void serviceRejectsNullImageEntryWithoutChangingBoard(boolean existingImage) {
        var board = seedBoardWithImages();
        var initial = boardFiles.findByRoommateBoard(board);
        var request = modifyRequest();
        if (existingImage) {
            request.setExistingImages(java.util.Collections.singletonList(null));
        } else {
            request.setExistingImages(List.of(new ExistingFileDto(initial.getFirst().getId(), true)));
            request.setNewImages(java.util.Collections.singletonList(null));
        }

        assertThatThrownBy(() -> service.modifyBoardPreSigned(member.getId(), board.getId(), request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_INVALID));
        TestTransaction.end();

        assertThat(boards.findById(board.getId()).orElseThrow().getTitle()).isEqualTo("원래 제목");
        assertThat(boardFiles.findAll()).extracting(RoommateBoardFile::getId)
                .containsExactlyInAnyOrderElementsOf(initial.stream().map(RoommateBoardFile::getId).toList());
        assertThat(boardFiles.findById(initial.getFirst().getId()).orElseThrow().getIsThumbnail()).isTrue();
        assertThat(files.count()).isEqualTo(2);
        assertThat(files.findAll()).noneMatch(File::getIsDeleted);
        verifyNoInteractions(storage, options);
    }

    @Test
    void nonOwnerCannotModifyOrCheckStorage() {
        var board = seedBoardWithImages();
        assertThatThrownBy(() -> service.modifyBoardPreSigned(Long.MAX_VALUE, board.getId(), modifyRequest()))
                .isInstanceOf(BusinessException.class);
        TestTransaction.end();
        assertThat(boards.findById(board.getId()).orElseThrow().getTitle()).isEqualTo("원래 제목");
        assertThat(boardFiles.count()).isEqualTo(2);
        verifyNoInteractions(storage);
    }

    @Test
    void deletedBoardCannotBeModified() {
        var board = seedBoardWithImages();
        boards.findById(board.getId()).orElseThrow().softDelete();
        TestTransaction.flagForCommit(); TestTransaction.end(); TestTransaction.start();
        assertThatThrownBy(() -> service.modifyBoardPreSigned(member.getId(), board.getId(), modifyRequest()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    private RoommateBoard seedBoardWithImages() {
        RoommateBoard board = boards.save(RoommateBoard.builder().member(member).roomType(roomType).region(region)
                .title("원래 제목").contents("원래 내용").deposit(1000).monthlyRent(50).managementCost(5).build());
        for (int i = 0; i < 2; i++) {
            File file = files.save(File.builder().type(FileType.ROOMMATE_BOARD_IMAGE).originalFileName("old-" + i + ".jpg")
                    .savedFileName("https://images.example.com/uploads/old-" + i + ".jpg").fileExt("jpg").build());
            boardFiles.save(RoommateBoardFile.builder().roommateBoard(board).file(file).isThumbnail(i == 0).build());
        }
        TestTransaction.flagForCommit(); TestTransaction.end(); TestTransaction.start();
        return board;
    }

    private BoardPreSignedModifyDto.Request modifyRequest() {
        var r = new BoardPreSignedModifyDto.Request();
        r.setTitle("수정 제목"); r.setContents("수정 내용"); r.setDeposit(900); r.setMonthlyRent(40); r.setManagementCost(4);
        r.setRoomTypeId(roomType.getId()); r.setRegionId(region.getId());
        return r;
    }

    private BoardPreSignedDto.Request request(List<BoardPreSignedDto.Request.FileDto> images) {
        var r = new BoardPreSignedDto.Request();
        r.setTitle("제목"); r.setContents("내용"); r.setDeposit(1000); r.setMountlyRent(50); r.setManagementCost(5);
        r.setRoomTypeId(roomType.getId()); r.setRegionId(region.getId()); r.setImages(images);
        return r;
    }
}
