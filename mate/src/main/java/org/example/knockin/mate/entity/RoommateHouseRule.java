package org.example.knockin.mate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;
import org.example.knockin.member.entity.Member;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_house_rule")
public class RoommateHouseRule extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE) private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "my_roommate_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MyRoommate myRoommate;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "contents", length = 500)
    private String contents;

    @ColumnDefault("false")
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    public void modify(String title, String contents) {
        this.title = title;
        this.contents = contents;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
