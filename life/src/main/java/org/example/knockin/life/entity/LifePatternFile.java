package org.example.knockin.life.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "life_pattern_file")
public class LifePatternFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "life_pattern_id", nullable = false)
    private LifePattern lifePattern;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    public void modifyFileId(Long fileId) {
        this.fileId = fileId;
    }
}
