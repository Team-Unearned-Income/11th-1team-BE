package org.example.knockin.room.service.impl;
 
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.repository.RoomProfileRepository;
import org.example.knockin.room.service.impl.RoomProfileServiceImpl;
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
class RoomProfileServiceImplTest {
 
    @Mock
    private RoomProfileRepository roomProfileRepository;
 
    @InjectMocks
    private RoomProfileServiceImpl roomProfileService;
 
    @Test
    @DisplayName("방 제공자 프로필 저장 테스트")
    void saveRoomOfferProfileTest() {
        RoomOfferProfile profile = mock(RoomOfferProfile.class);
        given(roomProfileRepository.save(profile)).willReturn(profile);
 
        RoomOfferProfile result = roomProfileService.save(profile);
 
        assertThat(result).isEqualTo(profile);
        verify(roomProfileRepository).save(profile);
    }
 
    @Test
    @DisplayName("방 구직자 프로필 저장 테스트")
    void saveRoomSeekerProfileTest() {
        RoomSeekerProfile profile = mock(RoomSeekerProfile.class);
        given(roomProfileRepository.save(profile)).willReturn(profile);
 
        RoomSeekerProfile result = roomProfileService.save(profile);
 
        assertThat(result).isEqualTo(profile);
        verify(roomProfileRepository).save(profile);
    }
 
    @Test
    @DisplayName("회원별 방 프로필 조회 테스트")
    void findByMemberTest() {
        Long memberId = 1L;
        RoomProfile profile = mock(RoomProfile.class);
        given(roomProfileRepository.findByMemberId(memberId)).willReturn(List.of(profile));
 
        List<RoomProfile> result = roomProfileService.findByMemberId(memberId);
 
        assertThat(result).hasSize(1);
        verify(roomProfileRepository).findByMemberId(memberId);
    }
 
    @Test
    @DisplayName("방 프로필 삭제 테스트")
    void deleteTest() {
        RoomProfile profile = mock(RoomProfile.class);
 
        RoomProfile result = roomProfileService.delete(profile);
 
        assertThat(result).isEqualTo(profile);
        verify(roomProfileRepository).delete(profile);
        verify(roomProfileRepository).flush();
    }
}
