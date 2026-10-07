package org.example.knockin.board.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoBoardDetailDto;
import org.example.knockin.board.dto.BoBoardListDto;
import org.example.knockin.meta.service.impl.AlarmServiceImpl;
import org.example.knockin.meta.service.impl.PushNotificationServiceImpl;
import org.example.knockin.board.dto.BoardDetailDto;
import org.example.knockin.board.dto.BoardDto;
import org.example.knockin.board.dto.BoardEditDto;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.board.dto.BoardModifyDto;
import org.example.knockin.board.service.RoommateBoardService;
import org.example.knockin.global.config.RoommateBoardPolicy;
import org.example.knockin.board.dto.BoardDetailDto.Response.Condition;
import org.example.knockin.board.dto.BoardDetailDto.Response.ConditionWeight;
import org.example.knockin.board.dto.BoardDetailDto.Response.FileDetailDto;
import org.example.knockin.board.dto.BoardDetailDto.Response.Lifestyle;
import org.example.knockin.board.dto.BoardDetailDto.Response.RoomExtraOptionInfo;
import org.example.knockin.board.dto.BoardDto.Request.FileDto;
import org.example.knockin.board.dto.BoardDto.Response;
import org.example.knockin.board.dto.BoardEditDto.Response.RegionInfo;
import org.example.knockin.board.dto.BoardEditDto.Response.RoomTypeInfo;
import org.example.knockin.board.dto.BoardModifyDto.Request.ExistingFileDto;
import org.example.knockin.board.dto.BoardModifyDto.Request.NewFileDto;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.board.dto.MyBoardListDto;
import org.example.knockin.declaration.dto.ReportDto;
import org.example.knockin.meta.entity.Alarm;
import org.example.knockin.meta.entity.AlarmSettingType;
import org.example.knockin.meta.entity.AlarmType;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.global.entity.BoardAlarmTemplate;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardBadgeType;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.board.entity.RoommateBoardOption;
import org.example.knockin.member.entity.Member;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.MetaErrorCode;
import org.example.knockin.global.exception.RoommateBoardErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.global.util.StringUtils;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.board.repository.row.BasicInfoRow;
import org.example.knockin.board.repository.row.BoardBaseRow;
import org.example.knockin.board.repository.row.BoardInterestCountRow;
import org.example.knockin.board.repository.row.BoardThumbnailRow;
import org.example.knockin.board.repository.row.EditFormRow;
import org.example.knockin.board.repository.row.MyRoommateBoardRow;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.example.knockin.util.service.RoommateScoreService;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.meta.service.impl.SearchServiceImpl;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoommateBoardServiceImpl implements RoommateBoardService {
    private final RoommateBoardRepository roommateBoardRepository;
    private final MemberServiceImpl memberService;
    private final MetaServiceImpl metaService;
    private final RoommateScoreService javaRoommateScoreV2Service;
    private final RoommateBoardFileServiceImpl roommateBoardFileService;
    private final PreferenceConditionServiceImpl preferenceConditionService;
    private final MemberLifePatternService memberLifePatternService;
    private final AuthenticationServiceImpl authenticationService;
    private final RoommateBoardOptionServiceImpl roommateBoardOptionService;
    private final RoommateBoardInterestServiceImpl roommateBoardInterestService;
    private final RoommateBoardPolicy roommateBoardPolicy;
    private final SearchServiceImpl searchServiceImpl;
    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;
    private final DeclarationServiceImpl declarationService;

    public RoommateBoard findById(Long id) {
        return roommateBoardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
    }

    @Override
    @Transactional
    public BoardDto.Response save(BoardDto.Request request, Long memberId, List<MultipartFile> files) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        RoomType roomType = metaService.findByRoomTypeId(request.getRoomTypeId());

        Region region = metaService.findByRegionId(request.getRegionId())
                .orElseThrow(() -> new BusinessException(MetaErrorCode.REGION_NOT_FOUND));

        List<FileDto> fileDtos = request.getImages();

        if (fileDtos != null && !fileDtos.isEmpty()) {
            validateImageMaxCount(fileDtos.size());
            validateThumbnailCount(fileDtos.stream().filter(FileDto::isThumbnail).count());
        }

        List<MultipartFile> imageFiles = toImageFilesFromSaveRequest(fileDtos, files);
        List<Boolean> thumbnails = toThumbnailsFromSaveRequest(fileDtos);
        RoommateBoard savedRoommateBoard = saveRoommateBoard(request, member, roomType, region);
        saveRoommateBoardFiles(savedRoommateBoard, imageFiles, thumbnails);

        roommateBoardOptionService.saveByExtraOptionsIds(savedRoommateBoard, request.getExtraOptionIds());

        return new BoardDto.Response(savedRoommateBoard.getUpdatedAt());
    }

    private void validateImageMaxCount(long imageCount) {
        int imageMaxCount = roommateBoardPolicy.getImageMaxCount();
        if (imageCount > imageMaxCount) {
            throw new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_EXCEEDED, imageMaxCount);
        }
    }

    private void validateThumbnailCount(long thumbnailCount) {
        int thumbnailImageMaxCount = roommateBoardPolicy.getThumbnailImageMaxCount();
        if (thumbnailCount != thumbnailImageMaxCount) {
            throw new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_FILE_COUNT_THUMBNAIL_EXCEEDED, thumbnailImageMaxCount);
        }
    }

    private RoommateBoard saveRoommateBoard(BoardDto.Request request, Member member, RoomType roomType, Region region) {
        RoommateBoard roommateBoard = RoommateBoard.builder()
                .member(member)
                .title(request.getTitle())
                .contents(request.getContents())
                .deposit(request.getDeposit())
                .monthlyRent(request.getMountlyRent())
                .managementCost(request.getManagementCost())
                .roomType(roomType)
                .region(region)
                .comeableDateNegotiable(request.getComeableDateNegotiable())
                .comeableDate(request.getComeableDate())
                .build();

        return roommateBoardRepository.save(roommateBoard);
    }

    private void saveRoommateBoardFiles(RoommateBoard roommateBoard, List<MultipartFile> imageFiles, List<Boolean> thumbnails) {
        if (imageFiles.isEmpty()) return;

        try {
            roommateBoardFileService.saveAll(roommateBoard, imageFiles, thumbnails);
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional
    public Slice<BoardListDto.Response> getBoardList(BoardListDto.Request request, Pageable pageable, @Nullable Long requesterId) {
        validateLikedOnlyRequest(request.getLikedOnly(), requesterId);
        saveSearchKeyword(requesterId, request.getKeyword());

        LocalDateTime endDate = LocalDateTime.now()
                .minusDays(roommateBoardPolicy.getComeableDateVisibleGraceDays());
        Slice<BoardBaseRow> baseRows = roommateBoardRepository.search(request, pageable, endDate, requesterId);

        if (baseRows.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, baseRows.hasNext());
        }

        List<Long> boardIds = baseRows.stream()
                .map(BoardBaseRow::boardId)
                .toList();
        List<Long> memberIds = baseRows.stream()
                .map(BoardBaseRow::memberId)
                .distinct()
                .toList();

        Map<Long, String> thumbnailByBoardId = roommateBoardFileService.findThumbnailsByBoardIds(boardIds).stream()
                .collect(Collectors.toMap(
                        BoardThumbnailRow::boardId,
                        BoardThumbnailRow::imageUrl,
                        (first, second) -> first
                ));
        Map<Long, List<AuthenticationType>> authenticationsByMemberId =
                authenticationService.findAcceptedByMemberIds(memberIds).stream()
                        .collect(Collectors.groupingBy(
                                MemberAuthenticationRow::memberId,
                                Collectors.mapping(MemberAuthenticationRow::type, Collectors.toList())
                        ));
        Set<Long> interestedBoardIds = findInterestedBoardIds(requesterId, boardIds);

        Map<Long, Long> activeInterestCountByBoardId =
                roommateBoardInterestService.findActiveInterestCountsByBoardIds(boardIds)
                .stream()
                .collect(Collectors.toMap(
                        BoardInterestCountRow::boardId,
                        BoardInterestCountRow::count,
                        (first, second) -> first));

        return baseRows.map(row -> toResponse(
                row,
                thumbnailByBoardId,
                authenticationsByMemberId,
                interestedBoardIds,
                activeInterestCountByBoardId
        ));
    }

    private void validateLikedOnlyRequest(Boolean likedOnly, @Nullable Long requesterId) {
        if (Boolean.TRUE.equals(likedOnly) && requesterId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private Set<Long> findInterestedBoardIds(@Nullable Long requesterId, List<Long> boardIds) {
        if (requesterId == null || boardIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(roommateBoardInterestService.findActiveBoardIdsByMemberIdAndBoardIds(requesterId, boardIds));
    }

    private void saveSearchKeyword(@Nullable Long requesterId, @Nullable String keyword) {
        if (requesterId == null || keyword == null || keyword.isBlank()) return;

        Member member = memberService.findByIdOrThrow(requesterId);
        searchServiceImpl.save(member, keyword);
    }

    private List<RoommateBoardBadgeType> getBadges(long activeInterestCount) {
        List<RoommateBoardBadgeType> badges = new ArrayList<>();
        if (activeInterestCount >= roommateBoardPolicy.getHotBadgeMinInterestCount()) {
            badges.add(RoommateBoardBadgeType.HOT);
        }
        return badges;
    }

    private BoardListDto.Response toResponse(
            BoardBaseRow row,
            Map<Long, String> thumbnailByBoardId,
            Map<Long, List<AuthenticationType>> authenticationsByMemberId,
            Set<Long> interestedBoardIds,
            Map<Long, Long> activeInterestCountByBoardId
    ) {
        return BoardListDto.Response.builder()
                .id(row.boardId())
                .imageUrl(thumbnailByBoardId.get(row.boardId()))
                .title(row.title())
                .deposit(row.deposit())
                .monthlyRent(row.monthlyRent())
                .managementCost(row.managementCost())
                .roomTypes(List.of(row.roomTypeName()))
                .comeableDate(row.comeableDate())
                .regionFullName(StringUtils.parseToRegionFullName(
                        row.grandParentRegionName(),
                        row.parentRegionName(),
                        row.regionName()
                ))
                .memberId(row.memberId())
                .memberName(row.memberName())
                .memberProfileImageUrl(row.memberProfileImageUrl())
                .memberAge(row.memberBirth() == null ? null : DateUtils.calculateAge(row.memberBirth()))
                .gender(row.memberGender())
                .authentications(authenticationsByMemberId.getOrDefault(row.memberId(), List.of()))
                .hits(row.hits())
                .badges(getBadges(activeInterestCountByBoardId.getOrDefault(row.boardId(), 0L)))
                .interested(interestedBoardIds.contains(row.boardId()))
                .createdAt(row.createdAt())
                .build();
    }

    @Override
    @Transactional
    public BoardDetailDto.Response getBoardDetail(Long boardId, Long memberId) {
        increaseHits(boardId);

        BasicInfoRow basicInfoRow = roommateBoardRepository.getBasicInfo(boardId)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));

        Long ownerId = basicInfoRow.memberId();

        List<BoardDetailDto.Response.FileDetailDto> images = roommateBoardFileService.findFileDetailDtoByBoardId(boardId);
        List<RoomExtraOptionInfo> roomExtraOptions = roommateBoardOptionService.findExtraOptionsByBoardId(boardId);
        List<Long> scoreLookupMemberIds = List.of(ownerId);
        List<MatchingLifestyleRow> lifestyleRows = memberLifePatternService.findMatchingRowByMemberIdsIn(scoreLookupMemberIds);
        List<MatchingPreferenceConditionRow> conditionRows = preferenceConditionService.findRowByMemberIdsIn(scoreLookupMemberIds);
        List<MatchingPreferenceConditionWeightRow> conditionWeightRows = preferenceConditionService.findWeightRowByMemberIdsIn(scoreLookupMemberIds);

        List<Lifestyle> lifestyles = lifestyleRows.stream()
                .filter(row -> Objects.equals(row.memberId(), ownerId))
                .map(this::toLifestyle)
                .toList();
        List<Condition> conditions = conditionRows.stream()
                .filter(row -> Objects.equals(row.memberId(), ownerId))
                .map(this::toCondition)
                .toList();
        List<ConditionWeight> conditionWeights = conditionWeightRows.stream()
                .filter(row -> Objects.equals(row.memberId(), ownerId))
                .map(this::toConditionWeight)
                .toList();
        Compatibility compatibility = javaRoommateScoreV2Service.calculateScore(memberId, ownerId);
        List<AuthenticationType> authenticationTypes = authenticationService.findTypesByMemberId(ownerId);

        boolean interested = roommateBoardInterestService.existsActiveByBoardIdAndMemberId(boardId, memberId);
        boolean mine = ownerId.equals(memberId);

        long activeInterestCount = roommateBoardInterestService.findActiveInterestCountsByBoardIds(List.of(boardId)).stream()
                .filter(row -> Objects.equals(row.boardId(), boardId))
                .map(BoardInterestCountRow::count)
                .findFirst()
                .orElse(0L);
        List<RoommateBoardBadgeType> badges = getBadges(activeInterestCount);

        return toResponse(
                basicInfoRow,
                images,
                roomExtraOptions,
                lifestyles,
                conditions,
                conditionWeights,
                authenticationTypes,
                compatibility,
                interested,
                mine,
                badges
        );
    }

    private Lifestyle toLifestyle(MatchingLifestyleRow row) {
        return Lifestyle.builder()
                .lifestyleId(row.lifestyleId())
                .name(row.name())
                .value(row.value())
                .description(row.description())
                .type(row.type())
                .imageUrl(row.imageUrl())
                .build();
    }

    private Condition toCondition(MatchingPreferenceConditionRow row) {
        return Condition.builder()
                .conditionId(row.conditionId())
                .name(row.name())
                .value(row.value())
                .description(row.description())
                .type(row.type())
                .imageUrl(row.imageUrl())
                .build();
    }

    private ConditionWeight toConditionWeight(MatchingPreferenceConditionWeightRow row) {
        return ConditionWeight.builder()
                .weightConditionId(row.conditionWeightId())
                .name(row.name())
                .imageUrl(row.imageUrl())
                .build();
    }

    private BoardDetailDto.Response toResponse(
            BasicInfoRow basicInfoRow,
            List<BoardDetailDto.Response.FileDetailDto> images,
            List<RoomExtraOptionInfo> roomExtraOptions,
            List<Lifestyle> lifestyles,
            List<Condition> conditions,
            List<ConditionWeight> conditionWeights,
            List<AuthenticationType> authentications,
            Compatibility compatibility,
            boolean interested,
            boolean mine,
            List<RoommateBoardBadgeType> badges
    ) {

        String regionFullName = StringUtils.parseToRegionFullName(
                basicInfoRow.grandParentRegionName(),
                basicInfoRow.parentRegionName(),
                basicInfoRow.regionName());

        int memberAge = DateUtils.calculateAge(basicInfoRow.birth());

        return BoardDetailDto.Response.builder()
                .boardId(basicInfoRow.boardId())
                .images(images)
                .title(basicInfoRow.title())
                .deposit(basicInfoRow.deposit())
                .managementCost(basicInfoRow.managementCost())
                .monthlyRent(basicInfoRow.monthlyRent())
                .roomTypeName(basicInfoRow.roomTypeName())
                .regionFullName(regionFullName)
                .createdAt(basicInfoRow.createdAt())
                .hits(basicInfoRow.hits())
                .contents(basicInfoRow.contents())
                .roomExtraOptions(roomExtraOptions)
                .lifeStyles(lifestyles)
                .conditions(conditions)
                .conditionWeights(conditionWeights)
                .memberId(basicInfoRow.memberId())
                .memberName(basicInfoRow.memberName())
                .memberProfileImageUrl(basicInfoRow.memberProfileImageUrl())
                .memberAge(memberAge)
                .gender(basicInfoRow.gender())
                .authentications(authentications)
                .compatibility(compatibility)
                .interested(interested)
                .comeableDateNegotiable(basicInfoRow.comeableDateNegotiable())
                .comeableDate(basicInfoRow.comeableDate())
                .mine(mine)
                .badges(badges)
                .build();
    }

    private void increaseHits(Long boardId) {
        int counts = roommateBoardRepository.increaseHitsById(boardId);
        if (counts == 0) throw new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND);
    }

    @Override
    public BoardEditDto.Response getEditForm(Long memberId, Long boardId) {
        EditFormRow row = roommateBoardRepository.getEditRow(boardId)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));

        List<FileDetailDto> images = roommateBoardFileService.findFileDetailDtoByBoardId(boardId);
        List<RoomExtraOptionInfo> roomExtraOptions = roommateBoardOptionService.findExtraOptionsByBoardId(boardId);

        List<Lifestyle> lifestyles = memberLifePatternService.findLifeStyleDtoByMemberId(memberId);
        List<Condition> conditions = preferenceConditionService.findAllConditionByMemberId(memberId);
        List<ConditionWeight> conditionWeights = preferenceConditionService.findAllConditionWeightByMemberId(memberId);

        return BoardEditDto.Response.builder()
                .images(images)
                .title(row.title())
                .deposit(row.deposit())
                .monthlyRent(row.monthlyRent())
                .managementCost(row.managementCost())
                .roomType(RoomTypeInfo.builder()
                        .roomTypeId(row.roomTypeId())
                        .name(row.roomTypeName())
                        .imageUrl(row.roomTypeImageUrl())
                        .build())
                .region(new RegionInfo(row.regionId(), StringUtils.parseToRegionFullName(
                        row.grandParentRegionName(),
                        row.parentRegionName(),
                        row.regionName())))
                .comeableDateNegotiable(row.comeableDateNegotiable())
                .comeableDate(row.comeableDate())
                .roomExtraOptions(roomExtraOptions)
                .contents(row.contents())
                .lifeStyles(lifestyles)
                .conditions(conditions)
                .conditionWeights(conditionWeights)
                .build();
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public BoardModifyDto.Response modify(Long memberId, Long boardId, BoardModifyDto.Request request,
                                          @Nullable List<MultipartFile> files) {
        RoommateBoard roommateBoard = roommateBoardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));

        checkingIsOwner(roommateBoard, memberId);

        RoomType roomType = metaService.findByRoomTypeId(request.getRoomTypeId());
        Region region = metaService.findByRegionId(request.getRegionId())
                .orElseThrow(() -> new BusinessException(MetaErrorCode.REGION_NOT_FOUND));

        roommateBoard.modifyBasicInfo(request);
        roommateBoard.modifyRoomType(roomType);
        roommateBoard.modifyRegion(region);

        List<RoommateBoardOption> existingBoardOptions = roommateBoardOptionService.findWithRoomExtraOptionByBoardId(boardId);
        roommateBoardOptionService.deleteByExtraOptionIds(existingBoardOptions, request.getDeleteExtraOptionIds());
        roommateBoardOptionService.saveByExtraOptionsIds(roommateBoard, request.getNewExtraOptionIds());

        List<RoommateBoardFile> existingBoardFiles = roommateBoardFileService.findAllByRoommateBoard(roommateBoard);
        Map<Long, ExistingFileDto> existingFileDtoMap = toExistingImageMap(request.getExistingImages());
        syncExistingImages(existingBoardFiles, existingFileDtoMap);

        List<NewFileDto> newFileDtos = request.getNewImages();
        saveNewBoardFiles(roommateBoard, newFileDtos, files);

        validateBoardFileResult(roommateBoard);

        return new BoardModifyDto.Response(LocalDateTime.now());
    }

    private void checkingIsOwner(RoommateBoard roommateBoard, Long memberId) {
        Member member = roommateBoard.getMember();
        if (!Objects.equals(member.getId(), memberId)) {
            throw new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_FORBIDDEN);
        }
    }

    private Map<Long, ExistingFileDto> toExistingImageMap(List<ExistingFileDto> existingImages) {
        if (existingImages == null || existingImages.isEmpty()) {
            return Map.of();
        }

        return existingImages.stream()
                .collect(Collectors.toMap(
                        ExistingFileDto::getBoardFileId,
                        Function.identity(),
                        (first, second) -> first)
                );
    }

    private void syncExistingImages(List<RoommateBoardFile> existingBoardFiles,
                                    Map<Long, ExistingFileDto> existingFileDtoMap) {
        for (RoommateBoardFile boardFile : existingBoardFiles) {
            ExistingFileDto dto = existingFileDtoMap.get(boardFile.getId());

            if (dto == null) {
                softDeleteBoardFile(boardFile);
            } else {
                boardFile.modifyIsThumbnail(dto.isThumbnail());
            }
        }
    }

    private void softDeleteBoardFile(RoommateBoardFile boardFile) {
        roommateBoardFileService.softDelete(boardFile);
    }

    private void saveNewBoardFiles(RoommateBoard roommateBoard, List<NewFileDto> fileDtos,
                                   @Nullable List<MultipartFile> files) {
        if (fileDtos == null || fileDtos.isEmpty()) {
            return;
        }

        try {
            roommateBoardFileService.saveAll(
                    roommateBoard,
                    toImageFilesFromModifyRequest(fileDtos, files),
                    toThumbnailsFromModifyRequest(fileDtos));
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private List<MultipartFile> toImageFilesFromSaveRequest(
            @Nullable List<FileDto> fileDtos,
            @Nullable List<MultipartFile> files) {
        if (fileDtos == null || fileDtos.isEmpty()) {
            return List.of();
        }

        Set<Integer> usedFileIndexes = new HashSet<>();
        return fileDtos.stream()
                .map(fileDto -> getImageFile(fileDto.getFileIndex(), files, usedFileIndexes))
                .toList();
    }

    private List<Boolean> toThumbnailsFromSaveRequest(@Nullable List<FileDto> fileDtos) {
        if (fileDtos == null || fileDtos.isEmpty()) {
            return List.of();
        }

        return fileDtos.stream()
                .map(FileDto::isThumbnail)
                .toList();
    }

    private List<MultipartFile> toImageFilesFromModifyRequest(
            List<NewFileDto> fileDtos,
            @Nullable List<MultipartFile> files) {
        Set<Integer> usedFileIndexes = new HashSet<>();
        return fileDtos.stream()
                .map(fileDto -> getImageFile(fileDto.getFileIndex(), files, usedFileIndexes))
                .toList();
    }

    private List<Boolean> toThumbnailsFromModifyRequest(List<NewFileDto> fileDtos) {
        return fileDtos.stream()
                .map(NewFileDto::isThumbnail)
                .toList();
    }

    private MultipartFile getImageFile(Integer fileIndex, @Nullable List<MultipartFile> files,
                                       Set<Integer> usedFileIndexes) {
        if (fileIndex == null || fileIndex < 0 || files == null || fileIndex >= files.size()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }

        if (!usedFileIndexes.add(fileIndex)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }

        return files.get(fileIndex);
    }

    private void validateBoardFileResult(RoommateBoard roommateBoard) {
        List<RoommateBoardFile> roommateBoardFiles = roommateBoardFileService.findAllByRoommateBoard(roommateBoard);
        if (roommateBoardFiles.isEmpty()) return;
        validateImageMaxCount(roommateBoardFiles.size());
        validateThumbnailCount(roommateBoardFiles.stream().filter(RoommateBoardFile::getIsThumbnail).count());
    }

    @Override
    public Page<MyBoardListDto.Response.BoardItem> getMyBoardList(Pageable pageable, Member member) {
        Page<MyRoommateBoardRow> rawPage = roommateBoardRepository.findMyBoardList(pageable, member);

        List<Long> boardIds = rawPage.stream().map(MyRoommateBoardRow::getBoardId).toList();
        List<Long> memberIds = rawPage.stream().map(MyRoommateBoardRow::getMemberId).distinct().toList();

        Map<Long, String> thumbnailByBoardId = roommateBoardFileService.findThumbnailsByBoardIds(boardIds).stream()
                .collect(Collectors.toMap(
                        BoardThumbnailRow::boardId,
                        BoardThumbnailRow::imageUrl,
                        (first, second) -> first
                ));
        Map<Long, List<AuthenticationType>> authenticationsByMemberId =
                authenticationService.findAcceptedByMemberIds(memberIds).stream()
                        .collect(Collectors.groupingBy(
                                MemberAuthenticationRow::memberId,
                                Collectors.mapping(MemberAuthenticationRow::type, Collectors.toList())
                        ));
        Set<Long> interestedBoardIds = findInterestedBoardIds(member.getId(), boardIds);

        Map<Long, Long> activeInterestCountByBoardId =
                roommateBoardInterestService.findActiveInterestCountsByBoardIds(boardIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BoardInterestCountRow::boardId,
                                BoardInterestCountRow::count,
                                (first, second) -> first));

        List<MyBoardListDto.Response.BoardItem> boardItems = rawPage.getContent().stream().map(row -> {
            return MyBoardListDto.Response.BoardItem.builder()
                    .id(row.getBoardId())
                    .imageUrl(thumbnailByBoardId.get(row.getBoardId()))
                    .title(row.getTitle())
                    .deposit(row.getDeposit())
                    .monthlyRent(row.getMonthlyRent())
                    .managementCost(row.getManagementCost())
                    .roomTypes(List.of(row.getRoomTypeName()))
                    .comeableDate(row.getComeableDate())
                    .regionFullName(StringUtils.parseToRegionFullName(
                            row.getGrandParentRegionName(),
                            row.getParentRegionName(),
                            row.getRegionName()
                    ))
                    .memberId(row.getMemberId())
                    .memberName(row.getMemberName())
                    .memberProfileImageUrl(row.getMemberProfileImageUrl())
                    .memberAge(row.getMemberBirth() == null ? null : DateUtils.calculateAge(row.getMemberBirth()))
                    .gender(row.getMemberGender())
                    .authentications(authenticationsByMemberId.getOrDefault(row.getMemberId(), List.of()))
                    .hits(row.getHits())
                    .badges(getBadges(activeInterestCountByBoardId.getOrDefault(row.getBoardId(), 0L)))
                    .interested(interestedBoardIds.contains(row.getBoardId()))
                    .createdAt(row.getCreatedAt())
                    .build();
        }).toList();

        return new PageImpl<>(boardItems, pageable, rawPage.getTotalElements());
    }

    @Override
    @Transactional
    public BoardDto.Response likeBoard(Long boardId, Long memberId) {
        RoommateBoard roommateBoard = roommateBoardRepository.findByIdForUpdate(boardId)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));

        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        roommateBoardInterestService.toggle(member, roommateBoard);

        return new BoardDto.Response(LocalDateTime.now());
    }

    @Override
    @Transactional
    public BoardDto.Response deleteBoard(Long boardId, Long memberId) {

        RoommateBoard roommateBoard = roommateBoardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));

        Member owner = roommateBoard.getMember();

        if (!memberId.equals(owner.getId())) {
            throw new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_FORBIDDEN);
        }

        roommateBoard.softDelete();
        roommateBoardRepository.save(roommateBoard);
        return new Response(LocalDateTime.now());
    }

    @Override
    @Transactional
    public ReportDto.Response reportBoard(ReportDto.Request request, Long boardId, Long memberId) {

        RoommateBoard roommateBoard = roommateBoardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));

        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        declarationService.reportBoard(roommateBoard, member, request.getContents());

        return new ReportDto.Response(LocalDateTime.now());
    }

    @Override
    public List<BoBoardListDto.Response.BoardInfo> findBackOfficeBoardList(Pageable pageable, BoBoardListDto.Request request) {
        return roommateBoardRepository.findBackOfficeBoardList(pageable, request);
    }

    @Override
    public BoBoardDetailDto.Response findBackOffcieBoard(Long id) {
        return roommateBoardRepository.findBackOffcieBoard(id);
    }

    @Transactional
    @Override
    public RoommateBoard deleteBackOfficeBoard(Long id, String rejectReason) {
        RoommateBoard roommateBoard = roommateBoardRepository.findById(id).orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        roommateBoard.softDelete(rejectReason);

        Member member = roommateBoard.getMember();
        Alarm alarm = Alarm.builder()
                .title(BoardAlarmTemplate.DELETE_BOARD.formatTitle())
                .contents(BoardAlarmTemplate.DELETE_BOARD.formatContents(rejectReason))
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), BoardAlarmTemplate.DELETE_BOARD.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, BoardAlarmTemplate.DELETE_BOARD.formatTitle(), BoardAlarmTemplate.DELETE_BOARD.formatContents(rejectReason), BoardAlarmTemplate.DELETE_BOARD.formatDeepLink());

        return roommateBoard;
    }

    @Transactional
    @Override
    public RoommateBoard recoverDeleteBoard(Long id) {
        RoommateBoard roommateBoard = roommateBoardRepository.findById(id).orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        roommateBoard.recoverDelete();

        Member member = roommateBoard.getMember();
        Alarm alarm = Alarm.builder()
                .title(BoardAlarmTemplate.RECOVER_BOARD.formatTitle())
                .contents(BoardAlarmTemplate.RECOVER_BOARD.formatContents())
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), BoardAlarmTemplate.RECOVER_BOARD.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION,BoardAlarmTemplate.RECOVER_BOARD.formatTitle(), BoardAlarmTemplate.RECOVER_BOARD.formatContents(), BoardAlarmTemplate.RECOVER_BOARD.formatDeepLink(roommateBoard.getId()));

        return roommateBoard;
    }

    private String getFullRegionName(Region regionEntity) {
        List<String> regionNames = new ArrayList<>();
        Region current = regionEntity;
        while (current != null) {
            regionNames.addFirst(current.getName());
            current = current.getParent();
        }
        return String.join(" ", regionNames);
    }
}
