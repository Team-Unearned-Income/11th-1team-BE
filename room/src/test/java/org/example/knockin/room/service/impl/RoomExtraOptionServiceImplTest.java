package org.example.knockin.room.service.impl;
 
import org.example.knockin.room.dto.MetaRoomAddOptionsDto;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.repository.RoomExtraOptionRepository;
import org.example.knockin.room.service.impl.RoomExtraOptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.List;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
 
@ExtendWith(MockitoExtension.class)
class RoomExtraOptionServiceImplTest {
 
    @Mock
    private RoomExtraOptionRepository roomExtraOptionRepository;
 
    @InjectMocks
    private RoomExtraOptionServiceImpl roomExtraOptionService;
 
    @Test
    @DisplayName("ID 목록으로 방 추가옵션 목록 조회 테스트")
    void findAllByIdTest() {
        List<Long> ids = List.of(1L);
        RoomExtraOption option = mock(RoomExtraOption.class);
        given(roomExtraOptionRepository.findAllById(ids)).willReturn(List.of(option));
 
        List<RoomExtraOption> result = roomExtraOptionService.findAllById(ids);
 
        assertThat(result).hasSize(1);
        verify(roomExtraOptionRepository).findAllById(ids);
    }
 
    @Test
    @DisplayName("삭제 상태별 방 추가옵션 목록 조회 테스트")
    void findAllByIsDeletedTest() {
        MetaRoomAddOptionsDto.Response.RoomAddOptionItem option = MetaRoomAddOptionsDto.Response.RoomAddOptionItem.builder()
                .id(1L).name("에어컨").image("http://example.com/icon.png").build();
        given(roomExtraOptionRepository.findAllByIsDeleted(false)).willReturn(List.of(option));
 
        List<MetaRoomAddOptionsDto.Response.RoomAddOptionItem> result = roomExtraOptionService.findAllByIsDeleted(false);
 
        assertThat(result).hasSize(1);
        verify(roomExtraOptionRepository).findAllByIsDeleted(false);
    }
}
