package org.example.knockin.verification.repository.impl;

import static org.example.knockin.verification.entity.QAuthentication.authentication;
import static org.example.knockin.member.entity.QMember.member;
import static org.example.knockin.verification.entity.QAuthenticationApprove.authenticationApprove;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.verification.dto.BoVerificationApproveListDto;
import org.example.knockin.verification.dto.BoVerificationCancelListDto;
import org.example.knockin.verification.dto.BoVerificationWaitingDetailDto;
import org.example.knockin.verification.dto.BoVerificationWaitingListDto;
import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.verification.repository.AuthenticationRepositoryCustom;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@NullMarked
@Repository
@RequiredArgsConstructor
public class AuthenticationRepositoryImpl implements AuthenticationRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<AuthenticationType> getAcceptedAuthenticationTypeByMemberId(Long memberId) {
        return jpaQueryFactory
                .select(authentication.type)
                .from(authentication)
                .where(
                        authentication.memberId.eq(memberId),
                        authentication.isAccepted.isTrue(),
                        authentication.isDeleted.isFalse()
                )
                .join(member).on(authentication.memberId.eq(member.id))
                .fetch();
    }

    @Override
    public List<MemberAuthenticationRow> findAcceptedByMemberIds(List<Long> memberIds) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        MemberAuthenticationRow.class,
                        authentication.memberId,
                        authentication.type
                ))
                .distinct()
                .from(authentication)
                .where(
                        authentication.memberId.in(memberIds),
                        authentication.isAccepted.isTrue(),
                        authentication.isDeleted.isFalse()
                )
                .fetch();
    }

    @Override
    public List<BoVerificationApproveListDto.Response.EmployeeAuthItem> findVerificationApproves(Pageable pageable) {
        return jpaQueryFactory.select(Projections.fields(BoVerificationApproveListDto.Response.EmployeeAuthItem.class,
                    authentication.id,
                    basicInformation.name,
                    authentication.type,
                    authentication.isAccepted,
                    authentication.email,
                    authenticationApprove.createdAt.as("createAt")
                )).from(authenticationApprove).join(authenticationApprove.authentication, authentication)
                .join(member).on(authentication.memberId.eq(member.id))
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(authenticationApprove.status.eq(ApproveType.ACCEPTED))
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<BoVerificationCancelListDto.Response.EmployeeAuthItem> findVerificationCancels(Pageable pageable) {
        return jpaQueryFactory.select(Projections.fields(BoVerificationCancelListDto.Response.EmployeeAuthItem.class,
                        authentication.id,
                        basicInformation.name,
                        authentication.type,
                        authentication.isAccepted,
                        authentication.email,
                        authenticationApprove.createdAt.as("createAt"),
                        authenticationApprove.rejectReason.as("description")
                )).from(authenticationApprove).join(authenticationApprove.authentication, authentication)
                .join(member).on(authentication.memberId.eq(member.id))
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(authenticationApprove.status.eq(ApproveType.REJECT))
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<BoVerificationWaitingListDto.Response.EmployeeAuthItem> findVerificationsList(Pageable pageable) {
        return jpaQueryFactory.select(Projections.fields(BoVerificationWaitingListDto.Response.EmployeeAuthItem.class,
                        authentication.id,
                        basicInformation.name,
                        authentication.type,
                        authentication.isAccepted,
                        authentication.email,
                        authenticationApprove.createdAt.as("createAt")
                )).from(authenticationApprove).join(authenticationApprove.authentication, authentication)
                .join(member).on(authentication.memberId.eq(member.id))
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(authenticationApprove.status.eq(ApproveType.PENDING))
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public BoVerificationWaitingDetailDto.Response findVerifications(Long id) {
        return jpaQueryFactory.select(Projections.fields(BoVerificationWaitingDetailDto.Response.class,
                        authentication.id,
                        basicInformation.name,
                        authentication.type,
                        authentication.isAccepted,
                        authentication.email,
                        authenticationApprove.createdAt.as("createAt")
                )).from(authenticationApprove)
                .join(authenticationApprove.authentication, authentication)
                .join(member).on(authentication.memberId.eq(member.id))
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(authentication.id.eq(id))
                .fetchFirst();
    }

    @Override
    public Optional<MyVerificationListDto.Response.AuthInfo> findVerificationList(Pageable pageable, Long memberId, AuthenticationType authenticationType) {
        return Optional.ofNullable(jpaQueryFactory
                .select(Projections.fields(MyVerificationListDto.Response.AuthInfo.class,
                        authenticationApprove.status,
                        authentication.email,
                        authentication.createdAt.as("createAt")
                ))
                .from(authentication)
                .leftJoin(authenticationApprove).on(authenticationApprove.authentication.eq(authentication))
                .where(
                        authentication.memberId.eq(memberId),
                        authentication.isDeleted.isFalse(),
                        authentication.type.eq(authenticationType)
                )
                .orderBy(authentication.createdAt.desc())
                .fetchFirst());
    }
}
