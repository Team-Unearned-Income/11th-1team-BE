package org.example.knockin.room.event;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.room.repository.row.MatchingOfferProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerRegionRow;
import org.example.knockin.room.repository.row.MatchingSeekerRoomTypeRow;

@Getter
public class MatchingRoomProfilesQueryEvent {
    private final List<Long> memberIds;
    private final Set<RoomProfileType> profileTypes;
    private Result result;

    public MatchingRoomProfilesQueryEvent(List<Long> memberIds, Set<RoomProfileType> profileTypes) {
        this.memberIds = List.copyOf(memberIds);
        this.profileTypes = Set.copyOf(profileTypes);
    }

    public void complete(Result result) {
        this.result = Objects.requireNonNull(result);
    }

    public Result getResult() {
        if (result == null) {
            throw new IllegalStateException("MatchingRoomProfilesQueryEvent was not handled synchronously");
        }
        return result;
    }

    public record Result(
            List<MatchingOfferProfileRow> offers,
            List<MatchingSeekerProfileRow> seekers,
            List<MatchingSeekerRegionRow> regions,
            List<MatchingSeekerRoomTypeRow> roomTypes
    ) {
        public Result {
            offers = List.copyOf(offers);
            seekers = List.copyOf(seekers);
            regions = List.copyOf(regions);
            roomTypes = List.copyOf(roomTypes);
        }
    }
}
