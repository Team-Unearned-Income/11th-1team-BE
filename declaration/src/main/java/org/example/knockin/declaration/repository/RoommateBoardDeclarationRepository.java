package org.example.knockin.declaration.repository;

import java.util.Optional;
import org.example.knockin.declaration.entity.RoommateBoardDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateBoardDeclarationRepository extends JpaRepository<RoommateBoardDeclaration, Long>, RoommateBoardDeclarationRepositoryCustom {
    Optional<RoommateBoardDeclaration> findByRoommateBoardIdAndMemberId(Long roommateBoardId, Long memberId);
    Optional<RoommateBoardDeclaration> findById(Long id);
}