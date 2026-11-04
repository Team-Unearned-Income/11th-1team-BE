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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_declaration")
public class MemberDeclaration extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_id", nullable = false)
    private Long reportedId;

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
