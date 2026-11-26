package org.example.knockin.room.repository;

import java.util.List;

public interface RoomProfileRepositoryCustom {
    boolean isExsitRoomProfile(Long memberId);
    void deleteForMembers(List<Long> memberIds);
}
