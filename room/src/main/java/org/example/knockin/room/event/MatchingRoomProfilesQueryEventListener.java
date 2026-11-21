package org.example.knockin.room.event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.room.event.MatchingRoomProfilesQueryEvent.Result;
import org.example.knockin.room.repository.row.MatchingOfferProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerRegionRow;
import org.example.knockin.room.repository.row.MatchingSeekerRoomTypeRow;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.room.service.impl.RoomOfferProfileServiceImpl;
import org.example.knockin.room.service.impl.RoomSeekerProfileServiceImpl;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingRoomProfilesQueryEventListener {
    private final RoomOfferProfileServiceImpl roomOfferProfileService;
    private final RoomSeekerProfileServiceImpl roomSeekerProfileService;

    @EventListener
    public void handle(MatchingRoomProfilesQueryEvent event) {
        List<Long> memberIds = event.getMemberIds();
        List<MatchingOfferProfileRow> offers = List.of();
        List<MatchingSeekerProfileRow> seekers = List.of();
        List<MatchingSeekerRegionRow> regions = List.of();
        List<MatchingSeekerRoomTypeRow> roomTypes = List.of();

        if (event.getProfileTypes().contains(RoomProfileType.OFFER)) {
            offers = roomOfferProfileService.findAllOfferProfileByMemberIdIn(memberIds);
        }
        if (event.getProfileTypes().contains(RoomProfileType.SEEKER)) {
            seekers = roomSeekerProfileService.findAllSeekerProfileByMemberIdIn(memberIds);
            regions = roomSeekerProfileService.findAllSeekerRegionByMemberIdIn(memberIds);
            roomTypes = roomSeekerProfileService.findAllSeekerRoomTypeByMemberIdIn(memberIds);
        }

        event.complete(new Result(offers, seekers, regions, roomTypes));
    }
}
