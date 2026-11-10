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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatting_score")
public class ChattingScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatting_required_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChattingRequired chattingRequired;

    @Column(name = "preference_condition_log_degree_id")
    private Long preferenceConditionLogDegreeId;

    @Column(name = "member_life_pattern_log_degree_id", nullable = false)
    private Long memberLifePatternLogDegreeId;

    @Column(name = "preference_condition_weight_log_degree_id")
    private Long preferenceConditionWeightLogDegreeId;

    @Column(name = "score", nullable = false)
    private Integer score;
}
