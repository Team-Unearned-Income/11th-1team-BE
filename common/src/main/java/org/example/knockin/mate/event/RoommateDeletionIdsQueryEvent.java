package org.example.knockin.mate.event;

import java.util.List;
import lombok.Getter;

@Getter
public class RoommateDeletionIdsQueryEvent {
    private final List<Long> memberIds;
    private final List<Long> chatRoomIds;
    private List<Long> matchingRequestIds;
    private List<Long> calendarIds;

    public RoommateDeletionIdsQueryEvent(List<Long> memberIds, List<Long> chatRoomIds) {
        this.memberIds = List.copyOf(memberIds);
        this.chatRoomIds = List.copyOf(chatRoomIds);
    }

    public void complete(List<Long> matchingRequestIds, List<Long> calendarIds) {
        List<Long> requests = List.copyOf(matchingRequestIds);
        List<Long> calendars = List.copyOf(calendarIds);
        this.matchingRequestIds = requests;
        this.calendarIds = calendars;
    }

    public List<Long> getMatchingRequestIds() {
        requireCompleted();
        return matchingRequestIds;
    }

    public List<Long> getCalendarIds() {
        requireCompleted();
        return calendarIds;
    }

    private void requireCompleted() {
        if (matchingRequestIds == null || calendarIds == null) {
            throw new IllegalStateException("RoommateDeletionIdsQueryEvent was not handled synchronously");
        }
    }
}
