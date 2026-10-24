package org.example.knockin.board.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardPreSignedDto;
import org.example.knockin.board.dto.BoardDetailDto.Response.FileDetailDto;
import org.example.knockin.board.dto.BoardPreSignedModifyDto;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.board.repository.RoommateBoardFileRepository;
import org.example.knockin.board.repository.row.BoardThumbnailRow;
import org.example.knockin.meta.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RoommateBoardFileServiceImpl {

    private final RoommateBoardFileRepository roommateBoardFileRepository;
    private final FileService fileService;

    public List<RoommateBoardFile> saveAll(RoommateBoard roommateBoard, List<MultipartFile> multipartFiles, List<Boolean> thumbnails) throws IOException {
        List<RoommateBoardFile> roommateBoardFiles = new ArrayList<>();
        for (int i = 0; i < multipartFiles.size(); i++) {
            File file = saveFile(multipartFiles.get(i));
            roommateBoardFiles.add(saveRoommateBoardFile(roommateBoard, file, thumbnails.get(i)));
        }
        return roommateBoardFiles;
    }

    public RoommateBoardFile save(RoommateBoard roommateBoard, MultipartFile multipartFile, Boolean thumbnail) throws IOException {
        File file = saveFile(multipartFile);
        return saveRoommateBoardFile(roommateBoard, file, thumbnail);
    }

    private File saveFile(MultipartFile multipartFile) throws IOException {
        return fileService.save(multipartFile, FileType.ROOMMATE_BOARD_IMAGE);
    }

    private RoommateBoardFile saveRoommateBoardFile(RoommateBoard roommateBoard, File file, Boolean thumbnail) {
        RoommateBoardFile roommateBoardFile = RoommateBoardFile.builder()
                .roommateBoard(roommateBoard)
                .file(file)
                .isThumbnail(thumbnail)
                .build();

        return roommateBoardFileRepository.save(roommateBoardFile);
    }

    public void saveAllPreSigned(RoommateBoard board, List<BoardPreSignedDto.Request.FileDto> images, Long memberId) {
        for (BoardPreSignedDto.Request.FileDto image : images) {
            File file = fileService.savePresigned(image.getFileKey(), image.getOriginalFilename(), memberId, FileType.ROOMMATE_BOARD_IMAGE);
            saveRoommateBoardFile(board, file, image.isThumbnail());
        }
    }

    public void saveAllModifyPreSigned(RoommateBoard board, List<BoardPreSignedModifyDto.Request.FileDto> images, Long memberId) {
        for (BoardPreSignedModifyDto.Request.FileDto image : images) {
            File file = fileService.savePresigned(image.getFileKey(), image.getOriginalFilename(), memberId, FileType.ROOMMATE_BOARD_IMAGE);
            saveRoommateBoardFile(board, file, image.isThumbnail());
        }
    }

    public List<FileDetailDto> findFileDetailDtoByBoardId(Long boardId) {
        return roommateBoardFileRepository.getFileDetailDtoByBoardId(boardId);
    }

    public List<BoardThumbnailRow> findThumbnailsByBoardIds(List<Long> boardIds) {
        return roommateBoardFileRepository.findThumbnailsByBoardIds(boardIds);
    }

    public List<RoommateBoardFile> findAllByRoommateBoard(RoommateBoard roommateBoard) {
        return roommateBoardFileRepository.findByRoommateBoard(roommateBoard);
    }

    public void softDelete(RoommateBoardFile roommateBoardFile) {
        File file = roommateBoardFile.getFile();
        file.softDelete();
        roommateBoardFileRepository.delete(roommateBoardFile);
    }

}
