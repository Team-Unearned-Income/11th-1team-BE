package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardOption;
import org.example.knockin.board.service.impl.RoommateBoardOptionServiceImpl;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MetaErrorCode;
import org.example.knockin.board.repository.RoommateBoardOptionRepository;
import org.example.knockin.room.service.impl.RoomExtraOptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 게시글 옵션 서비스")
class RoommateBoardOptionServiceImplTest {

    @Mock
    private RoomExtraOptionServiceImpl roomExtraOptionService;

    @Mock
    private RoommateBoardOptionRepository roommateBoardOptionRepository;

    @InjectMocks
    private RoommateBoardOptionServiceImpl roommateBoardOptionService;

    @Test
    @DisplayName("추가 옵션 ID가 있으면 중복을 제거해 조회하고 게시글 옵션을 저장한다")
    void saveByExtraOptionsIdsSavesOptionsWithUniqueIds() {
        // Given
        RoommateBoard board = RoommateBoard.builder().id(1L).build();
        RoomExtraOption parking = createRoomExtraOption(10L, "주차 가능");
        RoomExtraOption elevator = createRoomExtraOption(11L, "엘리베이터");
        when(roomExtraOptionService.findAllById(List.of(10L, 11L))).thenReturn(List.of(parking, elevator));
        when(roommateBoardOptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<RoommateBoardOption> result = roommateBoardOptionService.saveByExtraOptionsIds(
                board, List.of(10L, 10L, 11L));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(RoommateBoardOption::getRoommateBoard).containsOnly(board);
        assertThat(result).extracting(RoommateBoardOption::getRoomExtraOptionId)
                .containsExactly(parking.getId(), elevator.getId());
        verify(roomExtraOptionService).findAllById(List.of(10L, 11L));
        verify(roommateBoardOptionRepository).saveAll(any());
    }

    @Test
    @DisplayName("추가 옵션 ID가 없으면 옵션 조회와 저장을 하지 않고 빈 목록을 반환한다")
    void saveByExtraOptionsIdsReturnsEmptyWhenIdsAreEmpty() {
        // Given
        RoommateBoard board = RoommateBoard.builder().id(1L).build();

        // When
        List<RoommateBoardOption> result = roommateBoardOptionService.saveByExtraOptionsIds(board, List.of());

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(roomExtraOptionService, roommateBoardOptionRepository);
    }

    @Test
    @DisplayName("존재하지 않는 추가 옵션 ID가 있으면 추가 옵션 없음 예외를 던지고 저장하지 않는다")
    void saveByExtraOptionsIdsThrowsWhenExtraOptionDoesNotExist() {
        // Given
        RoommateBoard board = RoommateBoard.builder().id(1L).build();
        when(roomExtraOptionService.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(createRoomExtraOption(10L, "주차 가능")));

        // When & Then
        assertThatThrownBy(() -> roommateBoardOptionService.saveByExtraOptionsIds(board, List.of(10L, 11L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MetaErrorCode.EXTRA_OPTION_NOT_FOUND));
        verify(roommateBoardOptionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("삭제할 추가 옵션 ID가 있으면 기존 게시글 옵션 중 일치하는 항목만 삭제한다")
    void deleteByExtraOptionIdsDeletesOnlyMatchingOptions() {
        // Given
        RoommateBoardOption parkingOption = createBoardOption(10L);
        RoommateBoardOption elevatorOption = createBoardOption(11L);
        ArgumentCaptor<List<RoommateBoardOption>> deleteTargetsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        roommateBoardOptionService.deleteByExtraOptionIds(
                List.of(parkingOption, elevatorOption),
                List.of(10L, 999L));

        // Then
        verify(roommateBoardOptionRepository).deleteAll(deleteTargetsCaptor.capture());
        assertThat(deleteTargetsCaptor.getValue()).containsExactly(parkingOption);
    }

    @Test
    @DisplayName("삭제할 추가 옵션 ID가 없으면 게시글 옵션을 삭제하지 않는다")
    void deleteByExtraOptionIdsDoesNothingWhenIdsAreEmpty() {
        // Given
        RoommateBoardOption parkingOption = createBoardOption(10L);

        // When
        roommateBoardOptionService.deleteByExtraOptionIds(List.of(parkingOption), null);

        // Then
        verifyNoInteractions(roommateBoardOptionRepository);
    }

    private RoommateBoardOption createBoardOption(Long extraOptionId) {
        return RoommateBoardOption.builder()
                .roommateBoard(RoommateBoard.builder().id(1L).build())
                .roomExtraOptionId(createRoomExtraOption(extraOptionId, "옵션-" + extraOptionId).getId())
                .build();
    }

    private RoomExtraOption createRoomExtraOption(Long id, String name) {
        return RoomExtraOption.builder()
                .id(id)
                .name(name)
                .isDeleted(false)
                .build();
    }
}
