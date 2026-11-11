package org.example.knockin.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_room_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_member_chatting_room_member",
                columnNames = {"chatting_room_id", "member_id"}
        )
)
public class ChatRoomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatting_room_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChattingRoom chattingRoom;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_left", nullable = false)
    private Boolean isLeft = false;

    public static ChatRoomMember of(ChattingRoom chattingRoom, Long memberId) {
        return ChatRoomMember.builder()
                .chattingRoom(chattingRoom)
                .memberId(memberId)
                .isLeft(false)
                .build();
    }

    public void left() {
        isLeft = true;
    }
}
