package org.example.knockin.life.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.life.dto.BoLifeStylePatternDetailDto;
import org.example.knockin.life.dto.BoLifeStylePatternDto;
import org.example.knockin.life.dto.BoLifeStylePatternListDto;
import org.example.knockin.life.dto.MetaLifestylePatternsDto;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternFile;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.LifePatternErrorCode;
import org.example.knockin.life.repository.LifePatternFileRepository;
import org.example.knockin.life.repository.LifePatternInformationRepository;
import org.example.knockin.life.repository.LifePatternRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LifeStyleServiceImpl {
    private final LifePatternRepository lifePatternRepository;
    private final LifePatternFileRepository lifePatternFileRepository;
    private final LifePatternInformationRepository lifePatternInformationRepository;

    @Transactional
    public Long saveLifePatternLogic(BoLifeStylePatternDto.Request request) {

        LifePattern lifePattern = LifePattern.builder()
                .name(request.getName())
                .lifePatternDescription(request.getLifePatternDescription())
                .preferenceDescription(request.getPreferenceDescription())
                .dtype(request.getType())
                .sort(request.getSort())
                .build();

        lifePatternRepository.save(lifePattern);

        List<LifePatternInformation> informationList = request.getDetails().stream()
                .map(detailItem -> saveLifePatternInformation(lifePattern, detailItem.getValues(), detailItem.getDescription()))
                .toList();

        return lifePattern.getId();
    }

    private LifePattern saveLifePattern(LifePattern lifePattern) {
        return lifePatternRepository.save(lifePattern);
    }

    @Deprecated
    @Transactional
    public LifePatternFile saveLifePatternFile(LifePatternFile lifePatternFile) {
        return lifePatternFileRepository.save(lifePatternFile);
    }

    public void saveLifePatternFile(Long lifePatternId, Long fileId) {
        LifePattern lifeStyle = findLifeStyle(lifePatternId);
        LifePatternFile lifePatternFile = LifePatternFile.builder().lifePattern(lifeStyle).fileId(fileId).build();
        lifePatternFileRepository.save(lifePatternFile);
    }

    public LifePatternInformation saveLifePatternInformation(LifePattern lifePattern, String values, String description) {
        LifePatternInformation lifePatternInformation = LifePatternInformation.builder()
                .lifePattern(lifePattern)
                .dvalue(values)
                .description(description)
                .build();
        return lifePatternInformationRepository.save(lifePatternInformation);
    }

    @Transactional
    public void modifyLifePattern(BoLifeStylePatternDto.Request request, Long patternId) {
        LifePattern lifePattern = findLifeStyle(patternId);
        List<LifePatternInformation> existingList = findLifeInformationByPattern(lifePattern);
        List<BoLifeStylePatternDto.Request.DetailItem> details = request.getDetails();

        if (details != null) {
            int existingSize = existingList.size();
            int detailSize = details.size();

            for (int i = 0; i < detailSize; i++) {
                BoLifeStylePatternDto.Request.DetailItem detail = details.get(i);
                if (i < existingSize) {
                    LifePatternInformation existing = existingList.get(i);
                    existing.modifyLifePatternInformation(detail.getValues(), detail.getDescription());
                } else {
                    saveLifePatternInformation(lifePattern, detail.getValues(), detail.getDescription());
                }
            }

            if (existingSize > detailSize) {
                existingList.subList(detailSize, existingSize).forEach(this::deleteLifeInformation);
            }
        }

        lifePattern.modifyLifePattern(request.getName(), request.getType(), request.getSort(), request.getLifePatternDescription(), request.getPreferenceDescription());
    }

    @Transactional
    public List<LifePatternInformation> saveLifePatternInformation(List<LifePatternInformation> lifePatternInformation) {
        return lifePatternInformationRepository.saveAll(lifePatternInformation);
    }

    @Transactional
    public LifePatternInformation saveLifeInformation(LifePatternInformation lifePatternInformation) {
        return lifePatternInformationRepository.save(lifePatternInformation);
    }

    @Transactional
    public LifePattern deleteLifePattern(Long patternId) {
        LifePattern lifePattern = lifePatternRepository.findById(patternId).orElseThrow(() -> new BusinessException(LifePatternErrorCode.LIFE_PATTERN_NOT_FOUNT));
        lifePattern.deleteLifePattern();
        return lifePattern;
    }

    @Transactional
    public void deleteLifeInformationByPattern(LifePattern lifePattern) {
        lifePatternInformationRepository.deleteByLifePattern(lifePattern);
    }

    public List<LifePatternInformation> findLifeInformationByPattern(LifePattern lifePattern) {
        return lifePatternInformationRepository.findByLifePattern(lifePattern);
    }

    @Transactional
    public void deleteLifeInformation(LifePatternInformation lifePatternInformation) {
        lifePatternInformationRepository.delete(lifePatternInformation);
    }

    public BoLifeStylePatternListDto.Response findLifeStylePatternList(Pageable pageable) {
        List<BoLifeStylePatternListDto.Response.PatternItem> patternItemList = lifePatternRepository.findLifeStylePatternList(pageable);
        return BoLifeStylePatternListDto.Response.builder().patterns(patternItemList).build();
    }

    public BoLifeStylePatternDetailDto.Response findLifeStylePattern(Long patternId) {
        return lifePatternRepository.findLifeStylePattern(patternId);
    }

    public LifePattern findLifeStyle(Long patternId) {
        return lifePatternRepository.findById(patternId).orElseThrow(() -> new BusinessException(LifePatternErrorCode.LIFE_PATTERN_NOT_FOUNT));
    }

    public LifePatternFile findLifeStyleFile(LifePattern lifePattern) {
        return lifePatternFileRepository.findByLifePattern(lifePattern).orElse(null);
    }

    public boolean existsByLifePatternId(Long lifePatternId) {
        return lifePatternFileRepository.existsByLifePatternId(lifePatternId);
    }

    public void modifyLifePatternFile(Long lifePatternId, Long fileId) {
        LifePatternFile lifePatternFile = lifePatternFileRepository.findOneByLifePatternId(lifePatternId)
                .orElseThrow(() -> new BusinessException(LifePatternErrorCode.LIFE_PATTERN_FILE_NOT_FOUND));
        lifePatternFile.modifyFileId(fileId);
    }

    public List<LifePattern> findAllById(List<Long> lifeStyles) {
        if (lifeStyles == null || lifeStyles.isEmpty()) {
            return Collections.emptyList();
        }
        return lifePatternRepository.findAllById(lifeStyles);
    }

    public List<LifePatternInformation> findLifePatternInformationByIds(List<Long> lifeStyles) {
        if (lifeStyles == null || lifeStyles.isEmpty()) {
            return Collections.emptyList();
        }
        return lifePatternInformationRepository.findByLifeStyles(lifeStyles);
    }

    public List<MetaLifestylePatternsDto.Response.PatternItem> findLifeStylePatterns() {
        return lifePatternRepository.findLifeStylePatterns();
    }

    public List<LifePatternInformation> findLifePatternInformationAllById(List<Long> lifestyleIds) {
        if (lifestyleIds == null || lifestyleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return lifePatternInformationRepository.findAllById(lifestyleIds);
    }

    public LifePatternInformation findLifePatternInformationById(Long lifestyleId) {
        return lifePatternInformationRepository.findById(lifestyleId).orElse(null);
    }
}
