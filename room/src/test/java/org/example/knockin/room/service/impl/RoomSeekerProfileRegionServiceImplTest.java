package org.example.knockin.room.service.impl;
 
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.entity.RoomSeekerProfileRegion;
import org.example.knockin.room.repository.RoomSeekerProfileRegionRepository;
import org.example.knockin.room.service.impl.RoomSeekerProfileRegionServiceImpl;
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
class RoomSeekerProfileRegionServiceImplTest {
 
    @Mock
    private RoomSeekerProfileRegionRepository roomSeekerProfileRegionRepository;
 
    @InjectMocks
    private RoomSeekerProfileRegionServiceImpl roomSeekerProfileRegionService;
 
    @Test
    @DisplayName("구직자 매칭 지역 목록 모두 저장 테스트")
    void saveAllTest() {
        RoomSeekerProfileRegion region = mock(RoomSeekerProfileRegion.class);
        List<RoomSeekerProfileRegion> list = List.of(region);
        given(roomSeekerProfileRegionRepository.saveAll(list)).willReturn(list);
 
        List<RoomSeekerProfileRegion> result = roomSeekerProfileRegionService.saveAll(list);
 
        assertThat(result).hasSize(1);
        verify(roomSeekerProfileRegionRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("구직자별 매칭 지역 목록 삭제 테스트")
    void deleteByRoomSeekerProfileTest() {
        RoomSeekerProfile profile = mock(RoomSeekerProfile.class);
 
        RoomSeekerProfile result = roomSeekerProfileRegionService.deleteByRoomSeekerProfile(profile);
 
        assertThat(result).isEqualTo(profile);
        verify(roomSeekerProfileRegionRepository).deleteByRoomSeekerProfile(profile);
    }
}
