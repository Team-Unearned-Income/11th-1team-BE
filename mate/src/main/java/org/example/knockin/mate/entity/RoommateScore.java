package org.example.knockin.mate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_score")
public class RoommateScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "my_roommate_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MyRoommate myRoommate;

    @Column(name = "chatting_score_id")
    private Long chattingScoreId;
}
