package org.example.knockin.member.repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import org.example.knockin.member.dto.BoMemberDetailDto;
import org.example.knockin.member.dto.BoMemberListDto;
import org.example.knockin.life.dto.MyPreferencesAllDto;
import org.example.knockin.life.dto.MyProfileAllDto;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.meta.dto.AuthResponse;
import org.example.knockin.member.repository.row.MatchingBasicInfoRow;
import org.example.knockin.member.repository.row.MemberWithNameRow;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {
    Optional<Member> findMemberByProvider(String providerId, LoginProviderType providerType);
    Optional<AuthResponse> findMemberInfo(Member member);
    Optional<Member> findByProviderId(String providerId);
    List<Long> findDeletionTargetIdsForUpdate(List<Long> memberIds, LocalDateTime thresholdDate);
    long deleteMembersByIds(List<Long> memberIds);
    MyProfileAllDto.Response.UserInfo findByProfile(Member memberEntity);
    List<MyProfileAllDto.Response.Lifestyle> findByLifePattern(Member memberEntity);
    Optional<RoomProfile> findByRoomProfile(Member memberEntity);
    List<MyProfileAllDto.Response.RoomProfile> findRoomTypes(RoomProfile profile);
    List<Region> findSeekerRegionEntities(RoomSeekerProfile seeker);
    List<MyPreferencesAllDto.Response.Lifestyle> findPreferenceLifeStyle(Member member);
    List<MyPreferencesAllDto.Response.Condition> findPreferenceCondition(Member member);
    List<MatchingBasicInfoRow> findMatchingBasicRow(List<Long> excludeMemberIds, Integer size, @Nullable Long likedByMemberId, @Nullable Long requesterId);
    Optional<MatchingBasicInfoRow> findMatchingBasicRowById(Long memberId);
    List<BoMemberListDto.Response.MemberInfo> findBackOfficeMemberList(Pageable pageable, BoMemberListDto.Request request);
    BoMemberDetailDto.Response findBackOfficeMember(Long id);

    List<MemberWithNameRow> findAllWithNameRowById(List<Long> ids);
}
