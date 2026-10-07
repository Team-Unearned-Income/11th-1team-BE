package org.example.knockin.board.repository.impl;

import static org.example.knockin.board.entity.QRoommateBoard.roommateBoard;
import static org.example.knockin.board.entity.QRoommateBoardFile.roommateBoardFile;
import static org.example.knockin.board.entity.QRoommateBoardInterest.roommateBoardInterest;
import static org.example.knockin.member.entity.QBasicInformationFile.basicInformationFile;
import static org.example.knockin.meta.entity.QFile.file;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;
import static org.example.knockin.member.entity.QBlock.block;
import static org.example.knockin.member.entity.QMember.member;
import static org.example.knockin.member.entity.QState.state;
import static org.example.knockin.meta.entity.QRegion.region;
import static org.example.knockin.room.entity.QRoomType.roomType;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.repository.RoommateBoardRepositoryCustom;
import org.example.knockin.board.repository.row.BasicInfoRow;
import org.example.knockin.board.repository.row.BoardBaseRow;
import org.example.knockin.board.repository.row.EditFormRow;
import org.example.knockin.board.repository.row.MyRoommateBoardRow;
import org.example.knockin.board.dto.BoBoardDetailDto;
import org.example.knockin.board.dto.BoBoardListDto;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.global.util.QueryDslUtils;
import org.example.knockin.member.entity.QBasicInformation;
import org.example.knockin.member.entity.QBasicInformationFile;
import org.example.knockin.meta.entity.QFile;
import org.example.knockin.meta.entity.QRegion;
import org.example.knockin.room.entity.QRoomTypeFile;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class RoommateBoardRepositoryImpl implements RoommateBoardRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<BoardBaseRow> search(BoardListDto.Request request, Pageable pageable, LocalDateTime endDate, @Nullable Long requesterId) {
        QRegion boardRegion = new QRegion("searchBoardRegion");
        QRegion parentRegion = new QRegion("searchParentRegion");
        QRegion grandParentRegion = new QRegion("searchGrandParentRegion");
        QBasicInformation latestBasicInformation = new QBasicInformation("searchLatestBasicInformation");
        QBasicInformationFile latestBasicInformationFile = new QBasicInformationFile("searchLatestBasicInformationFile");
        String keyword = request.getKeyword();

        Predicate[] searchCondition = {
                keywordContains(keyword, boardRegion, parentRegion, grandParentRegion),
                regionIn(request.getRegionIds(), boardRegion, parentRegion, grandParentRegion),
                roomTypeIn(request.getRoomTypeIds()),
                genderEq(request.getGender()),
                depositBetween(request.getMinDeposit(), request.getMaxDeposit()),
                monthlyRentBetween(request.getMinMounthRent(), request.getMaxMounthRent()),
                isNotDeleted(),
                writerIsActive(),
                comeableDateNotExpired(endDate),
                likedOnly(request.getLikedOnly(), requesterId),
                notBlockedBetween(requesterId)
        };

        List<BoardBaseRow> content = jpaQueryFactory
                .select(Projections.constructor(
                        BoardBaseRow.class,
                        roommateBoard.id,
                        roommateBoard.title,
                        roommateBoard.deposit,
                        roommateBoard.monthlyRent,
                        roommateBoard.managementCost,
                        roommateBoard.comeableDate,
                        roommateBoard.hits,
                        roomType.name,
                        boardRegion.name,
                        parentRegion.name,
                        grandParentRegion.name,
                        member.id,
                        latestBasicInformation.name,
                        file.savedFileName,
                        latestBasicInformation.birth,
                        latestBasicInformation.gender,
                        roommateBoard.createdAt
                ))
                .from(roommateBoard)
                .join(roommateBoard.roomType, roomType)
                .join(roommateBoard.region, boardRegion)
                .leftJoin(boardRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .join(roommateBoard.member, member)
                .leftJoin(latestBasicInformation).on(latestBasicInformation.member.eq(member).and(latestBasicInformationIdEq(latestBasicInformation)))
                //.leftJoin(member.basicInformations, latestBasicInformation).on(latestBasicInformationIdEq(latestBasicInformation))
                .leftJoin(basicInformationFile)
                .on(basicInformationFile.id.eq(
                        JPAExpressions
                                .select(latestBasicInformationFile.id.max())
                                .from(latestBasicInformationFile)
                                .where(latestBasicInformationFile.basicInformation.id.eq(latestBasicInformation.id))
                ))
                .leftJoin(basicInformationFile.file, file)
                .on(file.isDeleted.isFalse())
                .where(searchCondition)
                .orderBy(toBoardOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();

        if (hasNext) {
            content.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression likedOnly(Boolean likedOnly, Long requesterId) {
        if (!Boolean.TRUE.equals(likedOnly)) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(roommateBoardInterest)
                .where(
                        roommateBoardInterest.roommateBoard.id.eq(roommateBoard.id),
                        roommateBoardInterest.member.id.eq(requesterId),
                        roommateBoardInterest.isDeleted.isFalse()
                )
                .exists();
    }

    private BooleanExpression notBlockedBetween(@Nullable Long requesterId) {
        if (requesterId == null) return null;

        BooleanExpression requesterBlock = JPAExpressions
                .selectOne()
                .from(block)
                .where(
                        block.isDeleted.isFalse(),
                        block.blocker.id.eq(requesterId),
                        block.blocked.id.eq(roommateBoard.member.id)
                )
                .notExists();

        BooleanExpression requesterBlocked = JPAExpressions
                .selectOne()
                .from(block)
                .where(
                        block.isDeleted.isFalse(),
                        block.blocked.id.eq(requesterId),
                        block.blocker.id.eq(roommateBoard.member.id)
                )
                .notExists();

        return requesterBlocked.and(requesterBlock);
    }

    private OrderSpecifier<?>[] toBoardOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : sort) {
            Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

            switch (order.getProperty()) {
                case "createdAt" -> orders.add(new OrderSpecifier<>(direction, roommateBoard.createdAt));
                case "hits" -> orders.add(new OrderSpecifier<>(direction, roommateBoard.hits));
                default -> throw new IllegalArgumentException("지원하지 않는 정렬 조건입니다.");
            }
        }

        if (orders.isEmpty()) orders.add(roommateBoard.createdAt.desc());
        orders.add(roommateBoard.id.desc());
        return orders.toArray(new OrderSpecifier[0]);
    }

    private BooleanExpression latestBasicInformationIdEq(QBasicInformation latestBasicInformation) {
        QBasicInformation maxBasicInformation = new QBasicInformation("searchMaxBasicInformation");
        return latestBasicInformation.id.eq(
                JPAExpressions
                        .select(maxBasicInformation.id.max())
                        .from(maxBasicInformation)
                        .where(maxBasicInformation.member.id.eq(member.id))
        );
    }

    @Override
    public Optional<BasicInfoRow> getBasicInfo(Long boardId) {
        QRegion boardRegion = new QRegion("boardRegion");
        QRegion parentRegion = new QRegion("parentRegion");
        QRegion grandParentRegion = new QRegion("grandParentRegion");

        BasicInfoRow basicInfo = jpaQueryFactory
                .select(Projections.constructor(
                        BasicInfoRow.class,
                        roommateBoard.id,
                        roommateBoard.title,
                        roommateBoard.deposit,
                        roommateBoard.managementCost,
                        roommateBoard.monthlyRent,
                        roommateBoard.roomType.name,
                        boardRegion.name,
                        parentRegion.name,
                        grandParentRegion.name,
                        roommateBoard.comeableDateNegotiable,
                        roommateBoard.comeableDate,
                        roommateBoard.createdAt,
                        roommateBoard.hits,
                        roommateBoard.contents,
                        member.id,
                        basicInformation.name,
                        file.savedFileName,
                        basicInformation.birth,
                        basicInformation.gender
                ))
                .from(roommateBoard)
                .where(
                        roommateBoard.id.eq(boardId),
                        roommateBoard.isDeleted.isFalse()
                )
                .join(roommateBoard.roomType, roomType)
                .join(roommateBoard.region, boardRegion)
                .leftJoin(boardRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .join(roommateBoard.member, member)
                // 1:N 관계이지만 서비스적으로 1:1 유지 가정
                .leftJoin(basicInformation)
                .on(basicInformation.id.eq(
                        JPAExpressions
                                .select(basicInformation.id.max())
                                .from(basicInformation)
                                .where(basicInformation.member.id.eq(member.id))
                ))
                // 1:N 관계이지만 서비스적으로 1:1 유지 가정
                .leftJoin(basicInformationFile)
                .on(basicInformationFile.id.eq(
                        JPAExpressions
                                .select(basicInformationFile.id.max())
                                .from(basicInformationFile)
                                .where(basicInformationFile.basicInformation.id.eq(basicInformation.id))
                ))
                .leftJoin(basicInformationFile.file, file)
                .on(file.isDeleted.isFalse())
                .fetchOne();

        return Optional.ofNullable(basicInfo);
    }

    @Override
    public Page<MyRoommateBoardRow> findMyBoardList(Pageable page, Member memberEntity) {
        QRegion boardRegion = new QRegion("searchBoardRegion");
        QRegion parentRegion = new QRegion("searchParentRegion");
        QRegion grandParentRegion = new QRegion("searchGrandParentRegion");
        QBasicInformation latestBasicInformation = new QBasicInformation("searchLatestBasicInformation");
        QBasicInformationFile latestBasicInformationFile = new QBasicInformationFile("searchLatestBasicInformationFile");

        List<MyRoommateBoardRow> content = jpaQueryFactory
                .select(Projections.constructor(
                        MyRoommateBoardRow.class,
                        roommateBoard.id,
                        roommateBoard.title,
                        roommateBoard.deposit,
                        roommateBoard.monthlyRent,
                        roommateBoard.managementCost,
                        roommateBoard.comeableDate,
                        roommateBoard.hits,
                        roomType.name,
                        boardRegion.name,
                        parentRegion.name,
                        grandParentRegion.name,
                        member.id,
                        latestBasicInformation.name,
                        file.savedFileName,
                        latestBasicInformation.birth,
                        latestBasicInformation.gender,
                        roommateBoard.createdAt
                ))
                .from(roommateBoard)
                .join(roommateBoard.roomType, roomType)
                .join(roommateBoard.region, boardRegion)
                .leftJoin(boardRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .join(roommateBoard.member, member)
                .leftJoin(latestBasicInformation).on(latestBasicInformation.member.eq(member).and(latestBasicInformationIdEq(latestBasicInformation)))
                //.leftJoin(member.basicInformations, latestBasicInformation).on(latestBasicInformationIdEq(latestBasicInformation))
                .leftJoin(basicInformationFile)
                .on(basicInformationFile.id.eq(
                        JPAExpressions
                                .select(latestBasicInformationFile.id.max())
                                .from(latestBasicInformationFile)
                                .where(latestBasicInformationFile.basicInformation.id.eq(latestBasicInformation.id))
                ))
                .leftJoin(basicInformationFile.file, file)
                .on(file.isDeleted.isFalse())
                .where(member.eq(memberEntity), roommateBoard.isDeleted.isFalse())
                .orderBy(toBoardOrderSpecifiers(page.getSort()))
                .offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(roommateBoard.count())
                .from(roommateBoard)
                .join(roommateBoard.roomType, roomType)
                .join(roommateBoard.region, boardRegion)
                .leftJoin(boardRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .where(roommateBoard.member.eq(memberEntity))
                .fetchOne();

        return new PageImpl<>(content, page, total == null ? 0 : total);
    }

    public Optional<EditFormRow> getEditRow(Long boardId) {
        QRegion boardRegion = new QRegion("boardRegion");
        QRegion parentRegion = new QRegion("parentRegion");
        QRegion grandParentRegion = new QRegion("grandParentRegion");
        QRoomTypeFile selectedRoomTypeFile = new QRoomTypeFile("selectedEditRoomTypeFile");
        QRoomTypeFile latestRoomTypeFile = new QRoomTypeFile("latestEditRoomTypeFile");
        QFile roomTypeImageFile = new QFile("editRoomTypeImageFile");

        return Optional.ofNullable(jpaQueryFactory
                .select(Projections.constructor(
                        EditFormRow.class,
                        roommateBoard.title,
                        roommateBoard.deposit,
                        roommateBoard.monthlyRent,
                        roommateBoard.managementCost,
                        roommateBoard.roomType.id,
                        roommateBoard.roomType.name,
                        roomTypeImageFile.savedFileName,
                        boardRegion.id,
                        boardRegion.name,
                        parentRegion.name,
                        grandParentRegion.name,
                        roommateBoard.comeableDateNegotiable,
                        roommateBoard.comeableDate,
                        roommateBoard.contents
                ))
                .from(roommateBoard)
                .where(
                        roommateBoard.id.eq(boardId),
                        roommateBoard.isDeleted.isFalse()
                )
                .join(roommateBoard.roomType, roomType)
                .leftJoin(selectedRoomTypeFile)
                .on(selectedRoomTypeFile.id.eq(
                        JPAExpressions
                                .select(latestRoomTypeFile.id.max())
                                .from(latestRoomTypeFile)
                                .where(latestRoomTypeFile.roomType.eq(roomType))
                ))
                .leftJoin(selectedRoomTypeFile.file, roomTypeImageFile)
                .on(roomTypeImageFile.isDeleted.isFalse())
                .join(roommateBoard.region, boardRegion)
                .leftJoin(boardRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .fetchOne());
    }

    @Override
    public List<BoBoardListDto.Response.BoardInfo> findBackOfficeBoardList(Pageable pageable, BoBoardListDto.Request request) {
        QRegion parent = new QRegion("parent");
        QRegion grandParent = new QRegion("grandParent");
        return jpaQueryFactory.select(Projections.fields(BoBoardListDto.Response.BoardInfo.class,
                        roommateBoard.id,
                        roommateBoard.title,
                        basicInformation.name.as("writer"),
                        roommateBoard.comeableDate,
                        roommateBoard.isDeleted,
                        roommateBoard.createdAt,
                        roommateBoard.comeableDateNegotiable,
                        Expressions.stringTemplate("concat(coalesce({0}, ''), ' ', coalesce({1}, ''), ' ', coalesce({2}, ''))",
                                grandParent.name, parent.name, region.name).as("region")
                )).from(roommateBoard).join(roommateBoard.member, member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .leftJoin(roommateBoard.region, region)
                .leftJoin(region.parent, parent)
                .leftJoin(parent.parent, grandParent)
                .where(
                        searchKeyword(request.getSearchKeyword()),
                        searchState(request.getIsDeleted())
                )
                .fetch();
    }

    @Override
    public BoBoardDetailDto.Response findBackOffcieBoard(Long id) {
        QRegion parent = new QRegion("parent");
        QRegion grandParent = new QRegion("grandParent");
        return jpaQueryFactory.select(Projections.fields(BoBoardDetailDto.Response.class,
                        file.savedFileName.as("thumbnailImage"),
                        roommateBoard.title,
                        basicInformation.name.as("writer"),
                        member.id.as("writerId"),
                        Expressions.stringTemplate("concat(coalesce({0}, ''), ' ', coalesce({1}, ''), ' ', coalesce({2}, ''))",
                                grandParent.name, parent.name, region.name).as("region"),
                        roommateBoard.deposit.longValue().as("deposit"),
                        roommateBoard.monthlyRent.longValue().as("monthlyRent"),
                        roommateBoard.comeableDateNegotiable,
                        roommateBoard.comeableDate,
                        roommateBoard.createdAt,
                        roommateBoard.hits,
                        roommateBoard.isDeleted
                ))
                .from(roommateBoard)
                .join(roommateBoard.member, member)
                .leftJoin(basicInformation)
                .on(basicInformation.member.eq(member))
                .leftJoin(roommateBoard.region, region)
                .leftJoin(region.parent, parent)
                .leftJoin(parent.parent, grandParent)
                .leftJoin(roommateBoardFile).on(roommateBoardFile.roommateBoard.eq(roommateBoard).and(roommateBoardFile.isThumbnail.isTrue()))
                .leftJoin(roommateBoardFile.file, file)
                .where(roommateBoard.id.eq(id))
                .fetchOne();
    }

    private BooleanExpression regionIn(List<Long> regionIds, QRegion boardRegion, QRegion parentRegion, QRegion grandParentRegion) {
        if (regionIds == null || regionIds.isEmpty()) return null;
        List<Long> uniqueIds = regionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (uniqueIds.isEmpty()) return null;
        return boardRegion.id.in(uniqueIds).or(parentRegion.id.in(uniqueIds)).or(grandParentRegion.id.in(uniqueIds));
    }

    private BooleanExpression keywordContains(
            String keyword,
            QRegion boardRegion,
            QRegion parentRegion,
            QRegion grandParentRegion
    ) {
        if (!StringUtils.hasText(keyword)) return null;
        String normalizedKeyword = keyword.trim();
        return roommateBoard.title.containsIgnoreCase(normalizedKeyword)
                .or(roomType.name.containsIgnoreCase(normalizedKeyword))
                .or(boardRegion.name.containsIgnoreCase(normalizedKeyword))
                .or(parentRegion.name.containsIgnoreCase(normalizedKeyword))
                .or(grandParentRegion.name.containsIgnoreCase(normalizedKeyword));
    }

    private BooleanExpression roomTypeIn(List<Long> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) return null;
        return roommateBoard.roomType.id.in(roomTypeIds);
    }

    private BooleanExpression genderEq(Gender gender) {
        if (gender == null) return null;
        QBasicInformation latestBasicInformation = new QBasicInformation("genderLatestBasicInformation");
        QBasicInformation maxBasicInformation = new QBasicInformation("genderMaxBasicInformation");
        return JPAExpressions
                .selectOne()
                .from(latestBasicInformation)
                .where(
                        latestBasicInformation.member.id.eq(roommateBoard.member.id),
                        latestBasicInformation.gender.eq(gender),
                        latestBasicInformation.id.eq(
                                JPAExpressions
                                        .select(maxBasicInformation.id.max())
                                        .from(maxBasicInformation)
                                        .where(maxBasicInformation.member.id.eq(roommateBoard.member.id))
                        )
                )
                .exists();
    }

    private BooleanExpression depositBetween(Integer minDeposit, Integer maxDeposit) {
        return QueryDslUtils.numberBetween(roommateBoard.deposit, minDeposit, maxDeposit);
    }

    private BooleanExpression monthlyRentBetween(Integer minMounthRent, Integer maxMounthRent) {
        return QueryDslUtils.numberBetween(roommateBoard.monthlyRent, minMounthRent, maxMounthRent);
    }

    private BooleanExpression isNotDeleted() {
        return roommateBoard.isDeleted.isFalse();
    }

    private BooleanExpression writerIsActive() {
        return JPAExpressions
                .selectOne()
                .from(state)
                .where(
                        state.member.id.eq(roommateBoard.member.id),
                        state.states.eq(MemberState.ACTIVE)
                )
                .exists();
    }

    private BooleanExpression comeableDateNotExpired(LocalDateTime endDate) {
        if (endDate == null) return null;
        return roommateBoard.comeableDateNegotiable.isTrue()
                .or(roommateBoard.comeableDate.goe(endDate));
    }

    private BooleanExpression searchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return searchTitle(keyword)
                .or(searchWriter(keyword))
                .or(searchRegion(keyword));
    }

    private BooleanExpression searchTitle(String title) {
        return title != null ? roommateBoard.title.contains(title) : null;
    }

    private BooleanExpression searchWriter(String writer) {
        return writer != null ? basicInformation.name.contains(writer) : null;
    }

    private BooleanExpression searchRegion(String regionName) {
        if (!StringUtils.hasText(regionName)) return null;
        QRegion rChild = new QRegion("rChild");
        QRegion rParent = new QRegion("rParent");
        QRegion rGrandParent = new QRegion("rGrandParent");

        return roommateBoard.region.id.in(JPAExpressions.select(rChild.id)
                        .from(rChild)
                        .leftJoin(rChild.parent, rParent)
                        .leftJoin(rParent.parent, rGrandParent)
                        .where(rChild.name.contains(regionName).or(rParent.name.contains(regionName)).or(rGrandParent.name.contains(regionName))));
    }

    private BooleanExpression searchState(Boolean isDeleted) {
        return isDeleted != null ? roommateBoard.isDeleted.eq(isDeleted) : null;
    }

}
