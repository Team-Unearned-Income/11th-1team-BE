package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.example.knockin.board.service.impl.RoommateBoardInterestServiceImpl;
import org.example.knockin.member.entity.Member;
import org.example.knockin.board.repository.RoommateBoardInterestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 게시글 관심 서비스")
class RoommateBoardInterestServiceImplTest {

    @Mock
    private RoommateBoardInterestRepository roommateBoardInterestRepository;

    @InjectMocks
    private RoommateBoardInterestServiceImpl roommateBoardInterestService;

    @Test
    @DisplayName("관심 이력이 없으면 삭제되지 않은 관심 게시글을 새로 저장한다")
    void toggleSavesActiveInterestWhenHistoryDoesNotExist() {
        // Given
        Member member = Member.builder().id(1L).build();
        RoommateBoard board = RoommateBoard.builder().id(10L).build();
        when(roommateBoardInterestRepository.findByRoommateBoardAndMemberId(board, member.getId()))
                .thenReturn(Optional.empty());
        ArgumentCaptor<RoommateBoardInterest> interestCaptor = ArgumentCaptor.forClass(RoommateBoardInterest.class);

        // When
        roommateBoardInterestService.toggle(member.getId(), board);

        // Then
        verify(roommateBoardInterestRepository).save(interestCaptor.capture());
        RoommateBoardInterest savedInterest = interestCaptor.getValue();
        assertThat(savedInterest.getMemberId()).isEqualTo(member.getId());
        assertThat(savedInterest.getRoommateBoard()).isSameAs(board);
        assertThat(savedInterest.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("관심 이력이 있으면 기존 관심 게시글의 삭제 상태를 토글하고 새로 저장하지 않는다")
    void toggleUpdatesExistingInterestWhenHistoryExists() {
        // Given
        Member member = Member.builder().id(1L).build();
        RoommateBoard board = RoommateBoard.builder().id(10L).build();
        RoommateBoardInterest interest = RoommateBoardInterest.builder()
                .memberId(member.getId())
                .roommateBoard(board)
                .isDeleted(false)
                .build();
        when(roommateBoardInterestRepository.findByRoommateBoardAndMemberId(board, member.getId()))
                .thenReturn(Optional.of(interest));

        // When
        roommateBoardInterestService.toggle(member.getId(), board);

        // Then
        assertThat(interest.getIsDeleted()).isTrue();
        verify(roommateBoardInterestRepository, never()).save(any(RoommateBoardInterest.class));
    }

    @Test
    @DisplayName("활성 관심 여부 조회는 게시글 ID와 회원 ID로 Repository에 위임한다")
    void existsActiveByBoardIdAndMemberIdDelegatesToRepository() {
        // Given
        when(roommateBoardInterestRepository.existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(true);

        // When
        boolean result = roommateBoardInterestService.existsActiveByBoardIdAndMemberId(10L, 1L);

        // Then
        assertThat(result).isTrue();
        verify(roommateBoardInterestRepository).existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(10L, 1L);
    }
}
