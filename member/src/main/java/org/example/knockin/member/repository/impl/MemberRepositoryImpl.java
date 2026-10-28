package org.example.knockin.member.repository.impl;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLSubQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Objects;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.member.dto.BoMemberDetailDto;
import org.example.knockin.member.dto.BoMemberListDto;
import org.example.knockin.life.dto.MyPreferencesAllDto;
import org.example.knockin.life.dto.MyProfileAllDto;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.meta.dto.AuthResponse;
import org.example.knockin.member.repository.MemberRepositoryCustom;
import org.example.knockin.member.repository.row.MatchingBasicInfoRow;
import org.example.knockin.member.repository.row.MemberWithNameRow;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.example.knockin.chat.entity.QChattingRequired.chattingRequired;
import static org.example.knockin.chat.entity.QChattingRequiredAlarm.chattingRequiredAlarm;
import static org.example.knockin.mate.entity.QRoommateMatchingRequired.roommateMatchingRequired;
import static org.example.knockin.mate.entity.QRoommateMatchingRequiredAlarm.roommateMatchingRequiredAlarm;
import static org.example.knockin.mate.entity.QRoommateCalendar.roommateCalendar;
import static org.example.knockin.mate.entity.QRoommateCalendarAlarm.roommateCalendarAlarm;
import static org.example.knockin.mate.entity.QMyRoommate.myRoommate;

import static org.example.knockin.member.entity.QBlock.block;
import static org.example.knockin.member.entity.QMember.member;
import static org.example.knockin.member.entity.QMemberInterest.memberInterest;
import static org.example.knockin.life.entity.QPreferenceCondition.preferenceCondition;
import static org.example.knockin.life.entity.QMemberLifePattern.memberLifePattern;
import static org.example.knockin.life.entity.QPreferenceConditionWeight.preferenceConditionWeight;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;
import static org.example.knockin.life.entity.QLifePatternInformation.lifePatternInformation;
import static org.example.knockin.life.entity.QLifePattern.lifePattern;
import static org.example.knockin.member.entity.QBasicInformationFile.basicInformationFile;
import static org.example.knockin.meta.entity.QFile.file;
import static org.example.knockin.room.entity.QRoomProfile.roomProfile;
import static org.example.knockin.room.entity.QOfferRoomType.offerRoomType;
import static org.example.knockin.room.entity.QSeekerRoomType.seekerRoomType;
import static org.example.knockin.room.entity.QRoomType.roomType;
import static org.example.knockin.meta.entity.QRegion.region;
import static org.example.knockin.room.entity.QRoomSeekerProfileRegion.roomSeekerProfileRegion;
import static org.example.knockin.member.entity.QState.state;
import static org.example.knockin.member.entity.QMemberPrivacy.memberPrivacy;
import static org.example.knockin.verification.entity.QAuthenticationApprove.authenticationApprove;
import static org.example.knockin.verification.entity.QAuthentication.authentication;
import static org.example.knockin.declaration.entity.QMemberDeclaration.memberDeclaration;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Member> findMemberByProvider(String providerId, LoginProviderType providerType) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(member)
                .where(providerIdEq(providerId), providerTypeEq(providerType))
                .fetchOne());
    }

    public Optional<AuthResponse> findMemberInfo(Member memberEntity) {
        AuthResponse response = jpaQueryFactory
                .select(Projections.fields(AuthResponse.class,
                        basicInformation.name.as("name"),
                        JPAExpressions.selectOne()
                                .from(memberLifePattern)
                                .where(memberLifePattern.memberId.eq(member.id))
                                .exists()
                                .and(JPAExpressions.selectOne()
                                        .from(basicInformation)
                                        .where(basicInformation.member.eq(member))
                                        .exists())
                                .and(JPAExpressions.selectOne()
                                        .from(roomProfile)
                                        .where(roomProfile.member.eq(member))
                                        .exists())
                                .as("basicInfo"),
                        JPAExpressions.selectOne()
                                .from(preferenceCondition)
                                .where(preferenceCondition.memberId.eq(member.id))
                                .exists()
                                .and(JPAExpressions.selectOne()
                                                .from(preferenceConditionWeight)
                                                .where(preferenceConditionWeight.memberId.eq(member.id))
                                                .exists())
                                .as("preferenceInfo"),
                        Projections.fields(AuthResponse.DeleteInfo.class,
                                new CaseBuilder().when(member.isDelete.isTrue().or(state.states.eq(MemberState.INACTIVE)))
                                        .then(true).otherwise(false).as("isDelete"),
                                state.rejectReason.as("reason")).as("deleteInfo")
                ))
                .from(member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .leftJoin(state).on(state.member.eq(member))
                .where(member.id.eq(memberEntity.getId()))
                .fetchOne();

        if (response != null && response.getDeleteInfo() != null) {
            if (memberEntity.isDelete()) {
                response.getDeleteInfo().setReason("탈퇴한 회원입니다.");
            }
        }

        return Optional.ofNullable(response);
    }

    @Override
    public MyProfileAllDto.Response.UserInfo findByProfile(Member memberEntity) {
        return jpaQueryFactory.select(Projections.fields(MyProfileAllDto.Response.UserInfo.class,
                        basicInformation.gender,
                        basicInformation.name,
                        basicInformation.email,
                        basicInformation.birth,
                        file.savedFileName.as("profile"),
                        memberPrivacy.type.as("memberPrivacyType")
                        ))
                .from(member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .leftJoin(basicInformationFile).on(basicInformationFile.basicInformation.eq(basicInformation))
                .leftJoin(basicInformationFile.file, file)
                .leftJoin(memberPrivacy).on(memberPrivacy.member.eq(member))
                .where(member.eq(memberEntity)).fetchOne();
    }

    @Override
    public List<MyProfileAllDto.Response.Lifestyle> findByLifePattern(Member memberEntity) {
        return jpaQueryFactory.select(Projections.fields(MyProfileAllDto.Response.Lifestyle.class,
                        memberLifePattern.id.as("id"),
                        lifePattern.id.as("lifestyleId"),
                        lifePattern.name.as("name"),
                        lifePatternInformation.dvalue.as("value"),
                        lifePatternInformation.description.as("description"),
                        lifePattern.dtype.as("type")
                ))
                .from(memberLifePattern)
                .leftJoin(memberLifePattern.lifePatternInformation, lifePatternInformation)
                .leftJoin(lifePatternInformation.lifePattern, lifePattern)
                .where(memberLifePattern.memberId.eq(memberEntity.getId())).fetch();
    }

    @Override
    public Optional<RoomProfile> findByRoomProfile(Member memberEntity) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(roomProfile)
                .where(roomProfile.member.eq(memberEntity))
                .orderBy(roomProfile.createdAt.desc())
                .fetchFirst());
    }

    @Override
    public List<MyProfileAllDto.Response.RoomProfile> findRoomTypes(RoomProfile profile) {
        if (profile instanceof RoomOfferProfile offer) {
            return jpaQueryFactory
                    .select(Projections.fields(MyProfileAllDto.Response.RoomProfile.class,
                            roomType.id.as("roomProfileId"),
                            roomType.name.as("roomProfileName")
                    ))
                    .from(offerRoomType)
                    .join(offerRoomType.roomType, roomType)
                    .where(offerRoomType.roomOfferProfile.eq(offer))
                    .fetch();

        } else if (profile instanceof RoomSeekerProfile seeker) {
            return jpaQueryFactory
                    .select(Projections.fields(MyProfileAllDto.Response.RoomProfile.class,
                            roomType.id.as("roomProfileId"),
                            roomType.name.as("roomProfileName")
                    ))
                    .from(seekerRoomType)
                    .join(seekerRoomType.roomType, roomType)
                    .where(seekerRoomType.roomSeekerProfile.eq(seeker))
                    .fetch();
        }

        return Collections.emptyList();
    }

    @Override
    public List<Region> findSeekerRegionEntities(RoomSeekerProfile seeker) {
        return jpaQueryFactory
                .select(region)
                .from(roomSeekerProfileRegion)
                .join(roomSeekerProfileRegion.region, region)
                .where(roomSeekerProfileRegion.roomSeekerProfile.eq(seeker))
                .fetch();
    }

    public List<MyPreferencesAllDto.Response.Lifestyle> findPreferenceLifeStyle(Member memberEntity) {
        return jpaQueryFactory.select(Projections.fields(MyPreferencesAllDto.Response.Lifestyle.class,
                        preferenceCondition.id.as("id"),
                        lifePattern.id.as("lifestyleId"),
                        lifePattern.name.as("name"),
                        lifePatternInformation.dvalue.as("value"),
                        lifePatternInformation.description.as("description"),
                        lifePattern.dtype.as("type")))
                .from(preferenceCondition)
                .join(preferenceCondition.lifePatternInformation, lifePatternInformation)
                .join(lifePatternInformation.lifePattern, lifePattern)
                .where(preferenceCondition.memberId.eq(memberEntity.getId()))
                .fetch();
    }

    public List<MyPreferencesAllDto.Response.Condition> findPreferenceCondition(Member memberEntity) {
        return jpaQueryFactory.select(Projections.fields(MyPreferencesAllDto.Response.Condition.class,
                        lifePattern.id.as("conditionsId"),
                        lifePattern.name.as("name")))
                .from(preferenceConditionWeight)
                .join(preferenceConditionWeight.lifePattern, lifePattern)
                .where(preferenceConditionWeight.memberId.eq(memberEntity.getId()))
                .fetch();
    }

    public Optional<Member> findByProviderId(String providerId) {
        return Optional.ofNullable(jpaQueryFactory.selectFrom(member).where(member.providerId.eq(providerId)).fetchOne());
    }

    @Override
    public long deleteMembersByDelete(List<Long> memberIds, LocalDateTime thresholdDate) {
        List<Long> eligibleIds = jpaQueryFactory.select(member.id).from(member).where(member.id.in(memberIds), member.isDelete.isTrue(), member.deletedAt.lt(thresholdDate)).orderBy(member.id.asc()).setLockMode(LockModeType.PESSIMISTIC_WRITE).fetch();

        if (eligibleIds.isEmpty()) return 0;

        JPQLSubQuery<Long> chatRequestIds = JPAExpressions.select(chattingRequired.id).from(chattingRequired).where(chattingRequired.requester.id.in(eligibleIds).or(chattingRequired.requestee.id.in(eligibleIds)));
        JPQLSubQuery<Long> matchingRequestIds = JPAExpressions.select(roommateMatchingRequired.id).from(roommateMatchingRequired).where(roommateMatchingRequired.requester.id.in(eligibleIds).or(roommateMatchingRequired.requestee.id.in(eligibleIds)).or(roommateMatchingRequired.chattingRoom.chattingRequired.id.in(chatRequestIds)));
        JPQLSubQuery<Long> calendarIds = JPAExpressions.select(roommateCalendar.id).from(roommateCalendar).join(roommateCalendar.myRoommate, myRoommate).where(myRoommate.roommateMatchingRequired.id.in(matchingRequestIds));

        jpaQueryFactory.delete(chattingRequiredAlarm).where(chattingRequiredAlarm.chattingRequired.id.in(chatRequestIds)).execute();
        jpaQueryFactory.delete(roommateMatchingRequiredAlarm).where(roommateMatchingRequiredAlarm.roommateMatchingRequired.id.in(matchingRequestIds)).execute();
        jpaQueryFactory.delete(roommateCalendarAlarm).where(roommateCalendarAlarm.roommateCalendar.id.in(calendarIds)).execute();

        return jpaQueryFactory.delete(member).where(member.id.in(eligibleIds)).execute();
    }

    @Override
    public List<MatchingBasicInfoRow> findMatchingBasicRow(List<Long> excludeMemberIds, Integer size, @Nullable Long likedByMemberId, @Nullable Long requesterId) {
        if (size <= 0) return List.of();

        NumberExpression<Double> randomOrder = Expressions.numberTemplate(Double.class, "function('random')");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingBasicInfoRow.class,
                        member.id,
                        file.savedFileName,
                        basicInformation.name,
                        basicInformation.birth,
                        basicInformation.gender,
                        roomProfile.id,
                        roomProfile.type
                ))
                .from(member)
                .where(
                        member.isDelete.isFalse(),
                        memberIsActive(),
                        memberPrivacy.type.eq(MemberPrivacyType.PUBLIC),
                        memberIdNotIn(excludeMemberIds),
                        likedBy(likedByMemberId),
                        notBlockedBetween(requesterId)
                )
                .leftJoin(memberPrivacy)
                .on(memberPrivacy.member.eq(member))
                .leftJoin(basicInformation)
                .on(basicInformation.id.eq(
                        JPAExpressions
                                .select(basicInformation.id.max())
                                .from(basicInformation)
                                .where(basicInformation.member.id.eq(member.id))
                ))
                .join(roomProfile)
                .on(roomProfile.id.eq(
                        JPAExpressions
                                .select(roomProfile.id.max())
                                .from(roomProfile)
                                .where(roomProfile.member.id.eq(member.id))
                ))
                .leftJoin(basicInformationFile)
                .on(basicInformationFile.id.eq(
                        JPAExpressions
                                .select(basicInformationFile.id.max())
                                .from(basicInformationFile)
                                .where(basicInformationFile.basicInformation.id.eq(basicInformation.id))
                ))
                .leftJoin(basicInformationFile.file, file)
                .on(file.isDeleted.isFalse())
                .orderBy(randomOrder.asc())
                .limit(size)
                .fetch();
    }

    private BooleanExpression likedBy(Long likedByMemberId) {
        if (likedByMemberId == null) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(memberInterest)
                .where(
                        memberInterest.sender.id.eq(likedByMemberId),
                        memberInterest.receiver.id.eq(member.id),
                        memberInterest.isDeleted.isFalse()
                )
                .exists();
    }

    @Override
    public Optional<MatchingBasicInfoRow> findMatchingBasicRowById(Long memberId) {
        return Optional.ofNullable(jpaQueryFactory
                .select(Projections.constructor(
                        MatchingBasicInfoRow.class,
                        member.id,
                        file.savedFileName,
                        basicInformation.name,
                        basicInformation.birth,
                        basicInformation.gender,
                        roomProfile.id,
                        roomProfile.type
                ))
                .from(member)
                .where(
                        member.id.eq(memberId),
                        member.isDelete.isFalse(),
                        memberPrivacy.type.eq(MemberPrivacyType.PUBLIC)
                )
                .leftJoin(memberPrivacy)
                .on(memberPrivacy.member.eq(member))
                .leftJoin(basicInformation)
                .on(basicInformation.id.eq(
                        JPAExpressions
                                .select(basicInformation.id.max())
                                .from(basicInformation)
                                .where(basicInformation.member.id.eq(member.id))
                ))
                .join(roomProfile)
                .on(roomProfile.id.eq(
                        JPAExpressions
                                .select(roomProfile.id.max())
                                .from(roomProfile)
                                .where(roomProfile.member.id.eq(member.id))
                ))
                .leftJoin(basicInformationFile)
                .on(basicInformationFile.id.eq(
                        JPAExpressions
                                .select(basicInformationFile.id.max())
                                .from(basicInformationFile)
                                .where(basicInformationFile.basicInformation.id.eq(basicInformation.id))
                ))
                .leftJoin(basicInformationFile.file, file)
                .on(file.isDeleted.isFalse())
                .fetchOne());
    }

    @Override
    public List<BoMemberListDto.Response.MemberInfo> findBackOfficeMemberList(Pageable pageable, BoMemberListDto.Request request) {
        return jpaQueryFactory.select(Projections.fields(BoMemberListDto.Response.MemberInfo.class,
                    member.id,
                    basicInformation.name,
                    basicInformation.email,
                    member.createdAt,
                    member.role,
                    state.states.as("state"),
                    authentication.type.as("authenticationType")
                )).from(member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .leftJoin(state).on(state.member.eq(member))
                .leftJoin(authentication).on(authentication.memberId.eq(member.id))
                .leftJoin(authenticationApprove).on(authenticationApprove.authentication.eq(authentication))
                .where(searchName(request.getSearchName()), searchState(request.getSearchState()), searchApproveType(request.getSearchApproveType()))
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
    }

    @Override
    public BoMemberDetailDto.Response findBackOfficeMember(Long id) {
        return jpaQueryFactory
                .from(member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .leftJoin(state).on(state.member.eq(member))
                .leftJoin(authentication).on(authentication.memberId.eq(member.id))
                .where(member.id.eq(id))
                .transform(groupBy(member.id).as(Projections.fields(BoMemberDetailDto.Response.class,
                        member.id,
                        basicInformation.name,
                        basicInformation.email,
                        member.createdAt,
                        member.role,
                        basicInformation.gender,
                        basicInformation.birth,
                        state.states.as("state"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberDeclaration.count().intValue())
                                .from(memberDeclaration).where(memberDeclaration.reportedId.eq(member.id)), "reportCount"),
                        list(Projections.fields(BoMemberDetailDto.Response.AuthenticationInfo.class,
                                authentication.type.as("authenticationType"),
                                authentication.email.as("authenticationEmail"))).as("authenticationInfoList")))).get(id);
    }

    @Override
    public List<MemberWithNameRow> findAllWithNameRowById(List<Long> ids) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        MemberWithNameRow.class,
                        member.id,
                        basicInformation.name
                ))
                .from(member)
                .leftJoin(basicInformation)
                .on(basicInformation.id.eq(
                        JPAExpressions
                                .select(basicInformation.id.max())
                                .from(basicInformation)
                                .where(basicInformation.member.id.eq(member.id))
                ))
                .where(member.id.in(ids))
                .fetch();
    }

    private BooleanExpression providerIdEq(String providerId) {
        return StringUtils.hasText(providerId) ? member.providerId.eq(providerId) : null;
    }

    private BooleanExpression providerTypeEq(LoginProviderType providerType) {
        return providerType != null ? member.providerType.eq(providerType) : null;
    }

    private BooleanExpression memberIsActive() {
        return JPAExpressions
                .selectOne()
                .from(state)
                .where(
                        state.member.id.eq(member.id),
                        state.states.eq(MemberState.ACTIVE)
                )
                .exists();
    }

    private BooleanExpression memberIdNotIn(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return null;
        }

        List<Long> filteredIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return filteredIds.isEmpty() ? null : member.id.notIn(filteredIds);
    }

    private BooleanExpression notBlockedBetween(@Nullable Long requesterId) {
        if (requesterId == null) {
            return null;
        }

        BooleanExpression requesterBlock = JPAExpressions
                .selectOne()
                .from(block)
                .where(
                        block.isDeleted.isFalse(),
                        block.blocker.id.eq(requesterId),
                        block.blocked.id.eq(member.id)
                )
                .notExists();

        BooleanExpression requesterBlocked = JPAExpressions
                .selectOne()
                .from(block)
                .where(
                        block.isDeleted.isFalse(),
                        block.blocked.id.eq(requesterId),
                        block.blocker.id.eq(member.id)
                )
                .notExists();

        return requesterBlocked.and(requesterBlock);
    }

    private BooleanExpression searchName(String name) {
        return StringUtils.hasText(name) ? basicInformation.name.contains(name) : null;
    }

    private BooleanExpression searchState(MemberState stateInfo) {
        return stateInfo != null ? state.states.eq(stateInfo) : null;
    }

    private BooleanExpression searchApproveType(ApproveType approveType) {
        return approveType != null ? authenticationApprove.status.eq(approveType) : null;
    }
}
