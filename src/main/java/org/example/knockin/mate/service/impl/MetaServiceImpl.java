package org.example.knockin.mate.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.agreement.dto.TermsDetailDto;
import org.example.knockin.agreement.dto.TermsListDto;
import org.example.knockin.agreement.service.impl.AgreementServiceImpl;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.life.service.impl.LifeStyleServiceImpl;
import org.example.knockin.life.dto.MetaLifestylePatternsDto;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.meta.dto.MetaRegionsDto;
import org.example.knockin.meta.dto.PopularSearchDto;
import org.example.knockin.meta.dto.PresignedUrlDto;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.meta.service.FileService;
import org.example.knockin.meta.service.impl.RegionServiceImpl;
import org.example.knockin.meta.service.impl.SearchServiceImpl;
import org.example.knockin.room.dto.MetaRoomAddOptionsDto;
import org.example.knockin.room.dto.MetaRoomTypesDto;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.service.impl.RoomExtraOptionServiceImpl;
import org.example.knockin.room.service.impl.RoomTypeServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetaServiceImpl {
    private final LifeStyleServiceImpl lifeStyleService;
    private final RegionServiceImpl regionService;
    private final RoomTypeServiceImpl roomTypeService;
    private final RoomExtraOptionServiceImpl roomExtraOptionService;
    private final AgreementServiceImpl agreementService;
    private final SearchServiceImpl searchService;
    private final FileService fileService;

    public List<AgreementLog> findByAgreementLogIsCurrent(List<Long> agreementIds) {
        return agreementService.findByAgreementLogIsCurrent(agreementIds);
    }

    public List<LifePatternInformation> findByLifeStyle(List<Long> lifeStyles) {
        return lifeStyleService.findByLifeStyles(lifeStyles);
    }

    public List<LifePattern> findLifePatternByLifeStyle(List<Long> lifeStyles) {
        return lifeStyleService.findAllById(lifeStyles);
    }

    public Optional<Region> findByRegionId(Long id) {
        return regionService.findById(id);
    }

    public List<Region> findByRegions(List<Long> regions) {
        return regionService.findByRegions(regions);
    }

    public List<Region> findByRegionAndChild(List<Long> regionIds) {
        return regionService.findByIdInWithChild(regionIds);
    }

    public RoomType findByRoomTypeId(Long roomTypeId) {
        return roomTypeService.findRoomType(roomTypeId);
    }

    public List<RoomType> findByRoomTypes(List<Long> roomTypes) {
        return roomTypeService.findByRoomTypes(roomTypes);
    }

    public List<RoomExtraOption> findRoomExtraOptionsByIdIn(List<Long> ids) {
        return roomExtraOptionService.findAllById(ids);
    }

    public TermsListDto.Response findTermList() {
        List<TermsListDto.Response.TermsItem> termsItemList = agreementService.findByAgreementsIsCurrentAndIsDeleted().stream().map(item ->
            TermsListDto.Response.TermsItem.builder().id(item.getId()).title(item.getTitle()).build()).toList();
        return TermsListDto.Response.builder().terms(termsItemList).build();
    }

    public TermsDetailDto.Response findTermDetail(Long termsId) {
        Agreement agreement = agreementService.findAgreement(termsId);
        return TermsDetailDto.Response.builder().id(agreement.getId()).contents(agreement.getContents()).build();
    }

    public PopularSearchDto.Response findPopSearch() {
        List<PopularSearchDto.Response.RankItem> rankItems = searchService.findPopSearch();
        IntStream.range(0, rankItems.size()).forEach(i -> rankItems.get(i).setId(i + 1L));
        return PopularSearchDto.Response.builder().rank(rankItems).build();
    }

    public MetaLifestylePatternsDto.Response findLifeStylePatterns() {
        return MetaLifestylePatternsDto.Response.builder().patterns(lifeStyleService.findLifeStylePatterns()).build();
    }

    public MetaRoomTypesDto.Response findRoomTypes() {
        return MetaRoomTypesDto.Response.builder()
                .roomType(roomTypeService.findAllByIsDeleted(false))
                .build();
    }

    public MetaRegionsDto.Response findRegions() {
        return MetaRegionsDto.Response.builder()
                .region(regionService.findAll().stream().map(item ->
                        MetaRegionsDto.Response.RegionItem.builder().id(item.getId()).name(item.getName()).parentId(item.getParent() != null ? item.getParent().getId() : null).build()).toList())
                .build();
    }

    public MetaRoomAddOptionsDto.Response findRoomAddOptions() {
        return MetaRoomAddOptionsDto.Response.builder()
                .roomAddOption(roomExtraOptionService.findAllByIsDeleted(false)).build();
    }

    public PresignedUrlDto.Response presignedUploadUrl(PresignedUrlDto.Request request, Long memberId) {
        return fileService.presignedUploadUrl(request, memberId);
    }
}
