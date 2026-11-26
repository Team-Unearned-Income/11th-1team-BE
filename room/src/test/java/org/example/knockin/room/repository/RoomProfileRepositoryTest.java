package org.example.knockin.room.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.example.knockin.room.entity.QOfferRoomType.offerRoomType;
import static org.example.knockin.room.entity.QSeekerRoomType.seekerRoomType;
import static org.example.knockin.room.entity.QRoomSeekerProfileRegion.roomSeekerProfileRegion;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.OfferRoomType;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.entity.RoomExtraOptionFile;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.entity.RoomSeekerProfileRegion;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.entity.RoomTypeFile;
import org.example.knockin.room.entity.SeekerRoomType;
import org.example.knockin.room.repository.row.MatchingOfferProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerRegionRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = RoomProfileRepositoryTest.JpaTestConfiguration.class)
class RoomProfileRepositoryTest {
    @Autowired private EntityManager entityManager;
    @Autowired private JPAQueryFactory queryFactory;
    @Autowired private RoomProfileRepository profileRepository;
    @Autowired private RoomOfferProfileRepository offerRepository;
    @Autowired private RoomSeekerProfileRepository seekerRepository;
    @Autowired private RoomTypeRepository typeRepository;
    @Autowired private RoomExtraOptionRepository optionRepository;

    @Test
    void queriesProfilesAndRegionHierarchyByExternalIds() {
        Region city = persist(Region.builder().name("서울").scope(1).build());
        Region district = persist(Region.builder().name("강남구").scope(2).parent(city).build());
        Region region = persist(Region.builder().name("역삼동").scope(3).parent(district).build());
        RoomType type = persist(RoomType.builder().name("원룸").build());
        RoomOfferProfile offer = persistOffer(10L, region.getId(), type);
        RoomSeekerProfile seeker = persistSeeker(20L, region.getId(), type);
        entityManager.flush();
        entityManager.clear();

        assertThat(profileRepository.findByMemberId(10L)).extracting(RoomProfile::getId)
                .containsExactly(offer.getId());
        assertThat(profileRepository.isExsitRoomProfile(10L)).isTrue();
        assertThat(profileRepository.isExsitRoomProfile(999L)).isFalse();
        assertThat(offerRepository.findAllOfferProfileByMemberIdIn(List.of(10L)))
                .extracting(MatchingOfferProfileRow::memberId, MatchingOfferProfileRow::regionName,
                        MatchingOfferProfileRow::parentRegionName, MatchingOfferProfileRow::grandParentRegionName,
                        MatchingOfferProfileRow::roomTypeName)
                .containsExactly(tuple(10L, "역삼동", "강남구", "서울", "원룸"));
        assertThat(seekerRepository.findAllSeekerProfileByMemberIdIn(List.of(20L)))
                .singleElement().satisfies(row -> {
                    assertThat(row.memberId()).isEqualTo(seeker.getMemberId());
                    assertThat(row.minDeposit()).isEqualTo(100);
                    assertThat(row.maxMonthlyRent()).isEqualTo(80);
                });
        assertThat(seekerRepository.findAllSeekerRegionByMemberIdIn(List.of(20L)))
                .extracting(MatchingSeekerRegionRow::memberId, MatchingSeekerRegionRow::regionName,
                        MatchingSeekerRegionRow::parentRegionName, MatchingSeekerRegionRow::grandParentRegionName)
                .containsExactly(tuple(20L, "역삼동", "강남구", "서울"));
        assertThat(seekerRepository.findAllSeekerRoomTypeByMemberIdIn(List.of(20L)))
                .singleElement().satisfies(row -> assertThat(row.roomTypeName()).isEqualTo("원룸"));
        assertThat(offerRepository.findAllOfferProfileByMemberIdIn(List.of(20L))).isEmpty();
        assertThat(seekerRepository.findAllSeekerRegionByMemberIdIn(List.of(10L))).isEmpty();
    }

    @Test
    void joinsImagesByFileIdAndKeepsItemsWithoutImages() {
        File image = persist(File.builder().type(FileType.ETC).originalFileName("room.png")
                .savedFileName("saved-room.png").fileExt("png").build());
        RoomType withImage = persist(RoomType.builder().name("원룸").build());
        RoomType withoutImage = persist(RoomType.builder().name("투룸").build());
        RoomExtraOption option = persist(RoomExtraOption.builder().name("풀옵션").build());
        persist(RoomTypeFile.builder().roomType(withImage).fileId(image.getId()).build());
        persist(RoomExtraOptionFile.builder().roomExtraOption(option).fileId(image.getId()).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(typeRepository.findAllByIsDeleted(false)).extracting("id", "image")
                .containsExactlyInAnyOrder(tuple(withImage.getId(), "saved-room.png"),
                        tuple(withoutImage.getId(), null));
        assertThat(optionRepository.findAllByIsDeleted(false)).extracting("id", "image")
                .containsExactly(tuple(option.getId(), "saved-room.png"));
    }

    @Test
    void deletesOnlyTargetMembersProfilesAndTheirChildren() {
        Region region = persist(Region.builder().name("서울").scope(1).build());
        RoomType type = persist(RoomType.builder().name("원룸").build());
        RoomOfferProfile removedOffer = persistOffer(10L, region.getId(), type);
        RoomSeekerProfile removedSeeker = persistSeeker(20L, region.getId(), type);
        RoomOfferProfile retainedOffer = persistOffer(30L, region.getId(), type);
        RoomSeekerProfile retainedSeeker = persistSeeker(40L, region.getId(), type);
        entityManager.flush();

        profileRepository.deleteForMembers(List.of());
        assertThat(profileRepository.count()).isEqualTo(4);
        profileRepository.deleteForMembers(List.of(10L, 20L));
        entityManager.clear();

        assertThat(profileRepository.findAll()).extracting(RoomProfile::getId)
                .containsExactlyInAnyOrder(retainedOffer.getId(), retainedSeeker.getId());
        assertThat(entityManager.find(RoomOfferProfile.class, removedOffer.getId())).isNull();
        assertThat(entityManager.find(RoomSeekerProfile.class, removedSeeker.getId())).isNull();
        assertThat(queryFactory.selectFrom(offerRoomType).fetch())
                .extracting(row -> row.getRoomOfferProfile().getId()).containsExactly(retainedOffer.getId());
        assertThat(queryFactory.selectFrom(seekerRoomType).fetch())
                .extracting(row -> row.getRoomSeekerProfile().getId()).containsExactly(retainedSeeker.getId());
        assertThat(queryFactory.selectFrom(roomSeekerProfileRegion).fetch())
                .extracting(row -> row.getRoomSeekerProfile().getId()).containsExactly(retainedSeeker.getId());
        assertThat(entityManager.find(Region.class, region.getId())).isNotNull();
        assertThat(typeRepository.findById(type.getId())).isPresent();
    }

    private RoomOfferProfile persistOffer(Long memberId, Long regionId, RoomType type) {
        RoomOfferProfile profile = persist(RoomOfferProfile.builder().memberId(memberId).regionId(regionId)
                .deposit(500).monthlyRent(50).comeableAt(LocalDateTime.of(2027, 1, 1, 0, 0)).build());
        persist(OfferRoomType.builder().roomOfferProfile(profile).roomType(type).build());
        return profile;
    }

    private RoomSeekerProfile persistSeeker(Long memberId, Long regionId, RoomType type) {
        RoomSeekerProfile profile = persist(RoomSeekerProfile.builder().memberId(memberId)
                .minDeposit(100).maxDeposit(1000).minMonthlyRent(30).maxMonthlyRent(80)
                .comeableAt(LocalDateTime.of(2027, 1, 1, 0, 0)).build());
        persist(SeekerRoomType.builder().roomSeekerProfile(profile).roomType(type).build());
        persist(RoomSeekerProfileRegion.builder().roomSeekerProfile(profile).regionId(regionId).build());
        return profile;
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan("org.example.knockin")
    @EnableJpaRepositories("org.example.knockin.room.repository")
    @EnableJpaAuditing
    static class JpaTestConfiguration {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }
}
