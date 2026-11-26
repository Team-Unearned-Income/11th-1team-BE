package org.example.knockin.room.service.impl;

import org.example.knockin.room.entity.RoomType;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.RoomTypeErrorCode;
import org.example.knockin.room.repository.RoomTypeRepository;
import org.example.knockin.room.service.impl.RoomTypeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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
@DisplayName("방 형태 관리 서비스 테스트")
class RoomTypeServiceImplTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    @Test
    @DisplayName("방 형태 등록 성공 테스트")
    void saveRoomTypeSuccessTest() {
        // given
        RoomType roomType = RoomType.builder().name("원룸").build();
        given(roomTypeRepository.save(roomType)).willReturn(roomType);

        // when
        RoomType result = roomTypeService.saveRoomType(roomType);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("원룸");
        verify(roomTypeRepository).save(roomType);
    }

    @Test
    @DisplayName("방 형태 수정 성공 테스트")
    void modifyRoomTypeSuccessTest() {
        // given
        Long id = 1L;
        RoomType existing = spy(RoomType.builder().id(id).name("원룸").build());
        RoomType updateInfo = RoomType.builder().name("투룸").build();

        given(roomTypeRepository.findById(id)).willReturn(Optional.of(existing));

        // when
        RoomType result = roomTypeService.modifyRoomType(updateInfo, id);

        // then
        assertThat(result).isNotNull();
        verify(existing).modifyRoomType(updateInfo);
        assertThat(result.getName()).isEqualTo("투룸");
    }

    @Test
    @DisplayName("방 형태 수정 시 대상을 찾을 수 없으면 BusinessException 발생")
    void modifyRoomTypeNotFoundTest() {
        // given
        Long id = 1L;
        RoomType updateInfo = RoomType.builder().name("투룸").build();
        given(roomTypeRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomTypeService.modifyRoomType(updateInfo, id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomTypeErrorCode.ROOM_TYPE_NOT_FOUND);
    }

    @Test
    @DisplayName("방 형태 삭제 성공 테스트")
    void deleteRoomTypeSuccessTest() {
        // given
        Long id = 1L;
        RoomType existing = spy(RoomType.builder().id(id).isDeleted(false).build());

        given(roomTypeRepository.findById(id)).willReturn(Optional.of(existing));

        // when
        RoomType result = roomTypeService.deleteRoomType(id);

        // then
        assertThat(result).isNotNull();
        verify(existing).deleteRoomType();
        assertThat(result.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("방 형태 목록 조회 성공 테스트")
    void findRoomTypeListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        RoomType roomType1 = RoomType.builder().id(1L).name("원룸").build();
        RoomType roomType2 = RoomType.builder().id(2L).name("투룸").build();

        given(roomTypeRepository.findAllByIsDeleted(false, pageable)).willReturn(new PageImpl<>(List.of(roomType1, roomType2)));

        // when
        List<RoomType> result = roomTypeService.findRoomTypeList(pageable);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("원룸");
        assertThat(result.get(1).getName()).isEqualTo("투룸");
    }

    @Test
    @DisplayName("방 형태 단건 조회 성공 테스트")
    void findRoomTypeSuccessTest() {
        // given
        Long id = 1L;
        RoomType roomType = RoomType.builder().id(id).name("원룸").build();
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType));

        // when
        RoomType result = roomTypeService.findRoomType(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("원룸");
    }

    @Test
    @DisplayName("방 형태 단건 조회 시 대상을 찾을 수 없으면 BusinessException 발생")
    void findRoomTypeNotFoundTest() {
        // given
        Long id = 1L;
        given(roomTypeRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomTypeService.findRoomType(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomTypeErrorCode.ROOM_TYPE_NOT_FOUND);
    }
}
