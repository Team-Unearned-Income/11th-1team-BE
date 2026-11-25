package org.example.knockin.room.entity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.example.knockin.room.dto.ModifyProfileRoomInfoDto;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_offer_profile")
@DiscriminatorValue(RoomProfileType.Values.OFFER)
@OnDelete(action = OnDeleteAction.CASCADE)
public class RoomOfferProfile extends RoomProfile {
    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "deposit", nullable = false)
    private Integer deposit;

    @Column(name = "monthly_rent", nullable = false)
    private Integer monthlyRent;

    public void updateOffer(ModifyProfileRoomInfoDto.Request request, Long regionId) {
        this.deposit = request.getDeposit();
        this.monthlyRent = request.getMounthRent();
        this.regionId = regionId;
        this.updateCommonInfo(request.isComeableAtNegotiable(), request.getComeEnableAt());
    }
}
