package org.example.knockin.life.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_life_pattern_log_degree")
public class MemberLifePatternLogDegree extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private Long degree;
}
