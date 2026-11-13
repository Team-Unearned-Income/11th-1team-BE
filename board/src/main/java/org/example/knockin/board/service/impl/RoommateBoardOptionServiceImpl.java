package org.example.knockin.board.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto.Response.RoomExtraOptionInfo;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardOption;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MetaErrorCode;
import org.example.knockin.board.repository.RoommateBoardOptionRepository;
import org.example.knockin.room.service.impl.RoomExtraOptionServiceImpl;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoommateBoardOptionServiceImpl {
    private final RoomExtraOptionServiceImpl roomExtraOptionService;
    private final RoommateBoardOptionRepository roommateBoardOptionRepository;

    public List<RoommateBoardOption> saveByExtraOptionsIds(RoommateBoard roommateBoard, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = ids.stream().distinct().toList();
        List<RoomExtraOption> roomExtraOptions = roomExtraOptionService.findAllById(uniqueIds);

        if (uniqueIds.size() != roomExtraOptions.size()) {
            throw new BusinessException(MetaErrorCode.EXTRA_OPTION_NOT_FOUND);
        }

        return saveAll(roommateBoard, roomExtraOptions);
    }

    public List<RoommateBoardOption> saveAll(RoommateBoard roommateBoard, List<RoomExtraOption> roomExtraOptions) {
        if (roomExtraOptions.isEmpty()) {
            return List.of();
        }

        List<RoommateBoardOption> roommateBoardOptions = roomExtraOptions.stream()
                .map(roomExtraOption -> RoommateBoardOption.builder()
                        .roommateBoard(roommateBoard)
                        .roomExtraOptionId(roomExtraOption.getId())
                        .build())
                .toList();

        return roommateBoardOptionRepository.saveAll(roommateBoardOptions);
    }

    public List<RoomExtraOptionInfo> findExtraOptionsByBoardId(Long boardId) {
        return roommateBoardOptionRepository.getExtraOptionsByBoardId(boardId);
    }

    public List<RoommateBoardOption> findWithRoomExtraOptionByBoardId(Long boardId) {
        return roommateBoardOptionRepository.findWithRoomExtraOptionByBoardId(boardId);
    }

    public void deleteAll(List<RoommateBoardOption> roommateBoardOptions) {
        if (roommateBoardOptions.isEmpty()) {
            return;
        }

        roommateBoardOptionRepository.deleteAll(roommateBoardOptions);
    }

    public void deleteByExtraOptionIds(List<RoommateBoardOption> roommateBoardOptions, List<Long> extraOptionIds) {
        if (extraOptionIds == null || extraOptionIds.isEmpty()) {
            return;
        }

        Map<Long, RoommateBoardOption> extraOptionIdMap = roommateBoardOptions.stream()
                .collect(Collectors.toMap(
                        option -> option.getRoomExtraOptionId(),
                        Function.identity(),
                        (first, second) -> first)
                );

        List<RoommateBoardOption> deleteTargets = extraOptionIds.stream()
                .map(extraOptionIdMap::get)
                .filter(Objects::nonNull)
                .toList();

        deleteAll(deleteTargets);
    }
}
