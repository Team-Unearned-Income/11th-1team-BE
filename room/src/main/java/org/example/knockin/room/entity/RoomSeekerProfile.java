package org.example.knockin.room.entity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.knockin.room.dto.ModifyProfileRoomInfoDto;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_seeker_profile")
@DiscriminatorValue(RoomProfileType.Values.SEEKER)
@OnDelete(action = OnDeleteAction.CASCADE)
public class RoomSeekerProfile extends RoomProfile {
    @Column(name = "min_deposit", nullable = false)
    private Integer minDeposit;

    @Column(name = "max_deposit", nullable = false)
    private Integer maxDeposit;

    @Column(name = "min_monthly_rent", nullable = false)
    private Integer minMonthlyRent;

    @Column(name = "max_monthly_rent", nullable = false)
    private Integer maxMonthlyRent;

    public void updateSeeker(ModifyProfileRoomInfoDto.Request request) {
        this.maxDeposit = request.getMaxDeposit();
        this.minDeposit = request.getMinDeposit();
        this.minMonthlyRent = request.getMinMounthRent();
        this.maxMonthlyRent = request.getMaxMounthRent();
        this.updateCommonInfo(request.isComeableAtNegotiable(), request.getComeEnableAt());
    }
}
