package org.example.knockin.declaration.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.knockin.global.entity.CreatedAtEntity;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "roommate_board_declaration")
public class RoommateBoardDeclaration extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "roommate_board_id", nullable = false)
    private Long roommateBoardId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    private DeclarationType declarationType;

    public void changeDeclarationType(DeclarationType declarationType) {
        this.declarationType = declarationType;
    }

    public void changeDeclarationType(DeclarationType declarationType, String reason) {
        this.declarationType = declarationType;
        this.reason = reason;
    }
}
