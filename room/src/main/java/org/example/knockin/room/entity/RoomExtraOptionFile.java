package org.example.knockin.room.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_extra_option_file")
public class RoomExtraOptionFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_extra_option_id", nullable = false)
    private RoomExtraOption roomExtraOption;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    public void modifyFile(Long fileId) {
        this.fileId = fileId;
    }
}
