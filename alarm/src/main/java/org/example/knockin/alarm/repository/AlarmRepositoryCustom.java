package org.example.knockin.alarm.repository;

import java.util.List;

public interface AlarmRepositoryCustom {
    void deleteForMembers(List<Long> memberIds, List<Long> chatRequestIds, List<Long> chatRoomIds);
}
