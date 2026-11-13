package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.example.knockin.meta.repository.FileRepository;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.board.service.impl.RoommateBoardFileServiceImpl;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.board.repository.RoommateBoardFileRepository;
import org.example.knockin.meta.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 게시글 파일 서비스")
class RoommateBoardFileServiceImplTest {

    @Mock
    private RoommateBoardFileRepository roommateBoardFileRepository;

    @Mock
    private FileService fileService;

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private RoommateBoardFileServiceImpl roommateBoardFileService;

    @Test
    @DisplayName("여러 이미지 파일과 썸네일 정보가 주어지면 파일과 게시글 파일을 순서대로 저장한다")
    void saveAllUploadsFilesAndSavesBoardFilesInOrder() throws IOException {
        // Given
        RoommateBoard board = RoommateBoard.builder().id(1L).build();
        MultipartFile thumbnailPart = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile roomPart = org.mockito.Mockito.mock(MultipartFile.class);
        File thumbnailFile = createFile("thumbnail.jpg");
        File roomFile = createFile("room.jpg");
        when(fileService.save(thumbnailPart, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(thumbnailFile);
        when(fileService.save(roomPart, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(roomFile);
        when(roommateBoardFileRepository.save(any(RoommateBoardFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<RoommateBoardFile> result = roommateBoardFileService.saveAll(
                board,
                List.of(thumbnailPart, roomPart),
                List.of(true, false));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(RoommateBoardFile::getRoommateBoard).containsOnly(board);
        assertThat(result).extracting(RoommateBoardFile::getFileId).containsExactly(thumbnailFile.getId(), roomFile.getId());
        assertThat(result).extracting(RoommateBoardFile::getIsThumbnail).containsExactly(true, false);
        verify(fileService).save(thumbnailPart, FileType.ROOMMATE_BOARD_IMAGE);
        verify(fileService).save(roomPart, FileType.ROOMMATE_BOARD_IMAGE);
    }

    @Test
    @DisplayName("게시글 파일을 삭제하면 연결된 파일을 삭제 상태로 바꾸고 게시글 파일을 삭제한다")
    void softDeleteMarksFileDeletedAndDeletesBoardFile() {
        // Given
        File file = createFile("room.jpg");
        RoommateBoardFile boardFile = RoommateBoardFile.builder()
                .roommateBoard(RoommateBoard.builder().id(1L).build())
                .fileId(file.getId())
                .isThumbnail(true)
                .build();

        // When
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        roommateBoardFileService.softDelete(boardFile);

        // Then
        assertThat(file.getIsDeleted()).isTrue();
        verify(roommateBoardFileRepository).delete(boardFile);
    }

    @Test
    @DisplayName("게시글 파일 상세 조회는 게시글 ID로 Repository에 위임한다")
    void findFileDetailDtoByBoardIdDelegatesToRepository() {
        // Given
        Long boardId = 1L;
        when(roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId)).thenReturn(List.of());

        // When
        roommateBoardFileService.findFileDetailDtoByBoardId(boardId);

        // Then
        verify(roommateBoardFileRepository).getFileDetailDtoByBoardId(boardId);
    }

    @Test
    @DisplayName("게시글 파일 저장은 생성한 게시글 파일의 핵심 값을 Repository에 전달한다")
    void saveStoresBoardFileWithUploadedFile() throws IOException {
        // Given
        RoommateBoard board = RoommateBoard.builder().id(1L).build();
        MultipartFile multipartFile = org.mockito.Mockito.mock(MultipartFile.class);
        File savedFile = createFile("room.jpg");
        when(fileService.save(multipartFile, FileType.ROOMMATE_BOARD_IMAGE)).thenReturn(savedFile);
        when(roommateBoardFileRepository.save(any(RoommateBoardFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<RoommateBoardFile> boardFileCaptor = ArgumentCaptor.forClass(RoommateBoardFile.class);

        // When
        RoommateBoardFile result = roommateBoardFileService.save(board, multipartFile, true);

        // Then
        verify(roommateBoardFileRepository).save(boardFileCaptor.capture());
        assertThat(boardFileCaptor.getValue().getRoommateBoard()).isSameAs(board);
        assertThat(boardFileCaptor.getValue().getFileId()).isEqualTo(savedFile.getId());
        assertThat(boardFileCaptor.getValue().getIsThumbnail()).isTrue();
        assertThat(result).isSameAs(boardFileCaptor.getValue());
    }

    private File createFile(String savedFileName) {
        return File.builder().id((long) savedFileName.hashCode())
                .type(FileType.ROOMMATE_BOARD_IMAGE)
                .originalFileName(savedFileName)
                .savedFileName(savedFileName)
                .fileExt("jpg")
                .build();
    }
}
