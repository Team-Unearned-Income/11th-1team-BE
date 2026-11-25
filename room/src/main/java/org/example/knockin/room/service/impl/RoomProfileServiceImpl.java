package org.example.knockin.room.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.repository.RoomProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomProfileServiceImpl {
    private final RoomProfileRepository roomProfileRepository;

    @Transactional
    public RoomOfferProfile save(RoomOfferProfile roomOfferProfile) {
        return roomProfileRepository.save(roomOfferProfile);
    }

    @Transactional
    public RoomSeekerProfile save(RoomSeekerProfile roomSeekerProfile) {
        return roomProfileRepository.save(roomSeekerProfile);
    }

    public List<RoomProfile> findByMemberId(Long memberId) {
        return roomProfileRepository.findByMemberId(memberId);
    }

    @Transactional
    public RoomProfile delete(RoomProfile roomProfile) {
        roomProfileRepository.delete(roomProfile);
        roomProfileRepository.flush();
        return roomProfile;
    }
}
