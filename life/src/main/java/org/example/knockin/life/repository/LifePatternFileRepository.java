package org.example.knockin.life.repository;

import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LifePatternFileRepository extends JpaRepository<LifePatternFile, Long> {
    Optional<LifePatternFile> findByLifePattern(LifePattern lifePattern);

    boolean existsByLifePatternId(Long lifePatternId);

    Optional<LifePatternFile> findOneByLifePatternId(Long lifePatternId);
}