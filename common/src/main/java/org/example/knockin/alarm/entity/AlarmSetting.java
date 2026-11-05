package org.example.knockin.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.member.entity.Member;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "alarm_setting")
public class AlarmSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) private Member member;

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Enumerated(EnumType.STRING)
    private AlarmSettingType alarmSettingType;

    public void updateEnable(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
