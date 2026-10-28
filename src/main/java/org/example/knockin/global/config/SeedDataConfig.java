package org.example.knockin.global.config;

import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.dto.MessageType;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.AgreementType;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.meta.entity.Alarm;
import org.example.knockin.meta.entity.AlarmSetting;
import org.example.knockin.meta.entity.AlarmSettingType;
import org.example.knockin.meta.entity.Notification;
import org.example.knockin.meta.entity.NotificationAlarm;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationApprove;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.meta.entity.Faq;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.declaration.entity.RoommateBoardDeclaration;
import org.example.knockin.board.entity.RoommateBoardFile;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.example.knockin.board.entity.RoommateBoardOption;
import org.example.knockin.chat.entity.ChatRoomFile;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredAlarm;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.member.entity.BasicInformationFile;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryCategory;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Block;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.declaration.entity.MemberDeclaration;
import org.example.knockin.member.entity.MemberInterest;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.meta.entity.Search;
import org.example.knockin.member.entity.State;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternFile;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePattern;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.entity.PreferenceCondition;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionLogDegree;
import org.example.knockin.life.entity.PreferenceConditionWeight;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.entity.PreferenceConditionWeightLogDegree;
import org.example.knockin.life.repository.LifePatternFileRepository;
import org.example.knockin.life.repository.LifePatternInformationRepository;
import org.example.knockin.life.repository.LifePatternRepository;
import org.example.knockin.life.repository.MemberLifePatternLogDegreeRepository;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.PreferenceConditionLogDegreeRepository;
import org.example.knockin.life.repository.PreferenceConditionLogRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogDegreeRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.payment.entity.Payment;
import org.example.knockin.payment.entity.PaymentStatus;
import org.example.knockin.payment.entity.PaymentType;
import org.example.knockin.payment.entity.Point;
import org.example.knockin.payment.entity.PointLog;
import org.example.knockin.payment.entity.VarianceType;
import org.example.knockin.mate.entity.ExcludeRoommateCalendar;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.room.entity.OfferRoomType;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.mate.entity.RepeatRoommateCalendar;
import org.example.knockin.mate.entity.RepeatType;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.entity.RoomExtraOptionFile;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.entity.RoomSeekerProfileRegion;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.entity.RoomTypeFile;
import org.example.knockin.mate.entity.RoommateCalendar;
import org.example.knockin.mate.entity.RoommateCalendarAlarm;
import org.example.knockin.mate.entity.RoommateCalendarCategory;
import org.example.knockin.mate.entity.RoommateCalendarMember;
import org.example.knockin.mate.entity.RoommateHouseRule;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateMatchingRequiredAlarm;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.mate.entity.RoommateScore;
import org.example.knockin.room.entity.SeekerRoomType;
import org.example.knockin.meta.entity.AppVersion;
import org.example.knockin.meta.entity.AuthEmail;
import org.example.knockin.meta.entity.PlatformType;
import org.example.knockin.meta.entity.UpdateType;
import org.example.knockin.declaration.entity.DeclarationType;
import org.example.knockin.agreement.repository.AgreementLogRepository;
import org.example.knockin.agreement.repository.AgreementRepository;
import org.example.knockin.agreement.repository.AgreementTypeRepository;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.example.knockin.meta.repository.AlarmRepository;
import org.example.knockin.meta.repository.AlarmSettingRepository;
import org.example.knockin.meta.repository.NotificationAlarmRepository;
import org.example.knockin.meta.repository.NotificationRepository;
import org.example.knockin.verification.repository.AuthenticationApproveRepository;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.example.knockin.meta.repository.FaqRepository;
import org.example.knockin.declaration.repository.RoommateBoardDeclarationRepository;
import org.example.knockin.board.repository.RoommateBoardFileRepository;
import org.example.knockin.board.repository.RoommateBoardInterestRepository;
import org.example.knockin.board.repository.RoommateBoardOptionRepository;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.chat.repository.ChatRoomFileRepository;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.example.knockin.chat.repository.ChatRoomMessageRepository;
import org.example.knockin.chat.repository.ChattingRequiredAlarmRepository;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.chat.repository.ChattingRoomRepository;
import org.example.knockin.chat.repository.ChattingScoreRepository;
import org.example.knockin.member.repository.BasicInformationFileRepository;
import org.example.knockin.meta.repository.FileRepository;
import org.example.knockin.inquiry.repository.InquiryCategoryRepository;
import org.example.knockin.inquiry.repository.InquiryCommentRepository;
import org.example.knockin.inquiry.repository.InquiryRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.BlockRepository;
import org.example.knockin.declaration.repository.MemberDeclarationRepository;
import org.example.knockin.member.repository.MemberInterestRepository;
import org.example.knockin.member.repository.MemberPrivacyRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.meta.repository.SearchRepository;
import org.example.knockin.member.repository.StateRepository;
import org.example.knockin.payment.repository.PaymentRepository;
import org.example.knockin.payment.repository.PointLogRepository;
import org.example.knockin.payment.repository.PointRepository;
import org.example.knockin.mate.repository.ExcludeRoommateCalendarRepository;
import org.example.knockin.mate.repository.MyRoommateRepository;
import org.example.knockin.room.repository.OfferRoomTypeRepository;
import org.example.knockin.meta.repository.RegionRepository;
import org.example.knockin.mate.repository.RepeatRoommateCalendarRepository;
import org.example.knockin.room.repository.RoomExtraOptionFileRepository;
import org.example.knockin.room.repository.RoomExtraOptionRepository;
import org.example.knockin.room.repository.RoomOfferProfileRepository;
import org.example.knockin.room.repository.RoomSeekerProfileRegionRepository;
import org.example.knockin.room.repository.RoomSeekerProfileRepository;
import org.example.knockin.room.repository.RoomTypeFileRepository;
import org.example.knockin.room.repository.RoomTypeRepository;
import org.example.knockin.mate.repository.RoommateCalendarAlarmRepository;
import org.example.knockin.mate.repository.RoommateCalendarCategoryRepository;
import org.example.knockin.mate.repository.RoommateCalendarMemberRepository;
import org.example.knockin.mate.repository.RoommateCalendarRepository;
import org.example.knockin.mate.repository.RoommateHouseRuleRepository;
import org.example.knockin.mate.repository.RoommateMatchingRequiredAlarmRepository;
import org.example.knockin.mate.repository.RoommateMatchingRequiredRepository;
import org.example.knockin.mate.repository.RoommateScoreRepository;
import org.example.knockin.room.repository.SeekerRoomTypeRepository;
import org.example.knockin.meta.repository.AppVersionRepository;
import org.example.knockin.meta.repository.AuthEmailRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!prod")
public class SeedDataConfig implements CommandLineRunner {
    @Value("${seed.load-test.records-per-entity:0}")
    private int loadTestRecordsPerEntity;

    private final AgreementRepository agreementRepository;
    private final AgreementLogRepository agreementLogRepository;
    private final LifePatternRepository lifePatternRepository;
    private final LifePatternInformationRepository lifePatternInformationRepository;
    private final RegionRepository regionRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomExtraOptionRepository roomExtraOptionRepository;
    private final InquiryCategoryRepository inquiryCategoryRepository;
    private final AgreementTypeRepository agreementTypeRepository;

    private final AppVersionRepository appVersionRepository;
    private final AuthEmailRepository authEmailRepository;
    private final FaqRepository faqRepository;
    private final MemberRepository memberRepository;
    private final BasicInformationRepository basicInformationRepository;
    private final AuthenticationRepository authenticationRepository;
    private final AuthenticationApproveRepository authenticationApproveRepository;
    private final RoommateBoardRepository roommateBoardRepository;
    private final RoommateBoardOptionRepository roommateBoardOptionRepository;
    private final SearchRepository searchRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;

    private final MemberAgreementRepository memberAgreementRepository;
    private final MemberPrivacyRepository memberPrivacyRepository;
    private final MemberLifePatternRepository memberLifePatternRepository;
    private final PreferenceConditionRepository preferenceConditionRepository;
    private final PreferenceConditionWeightRepository preferenceConditionWeightRepository;
    private final StateRepository stateRepository;
    private final BlockRepository blockRepository;
    private final MemberDeclarationRepository memberDeclarationRepository;
    private final MemberInterestRepository memberInterestRepository;
    private final FileRepository fileRepository;
    private final BasicInformationFileRepository basicInformationFileRepository;
    private final RoommateBoardFileRepository roommateBoardFileRepository;
    private final RoomOfferProfileRepository roomOfferProfileRepository;
    private final RoomSeekerProfileRepository roomSeekerProfileRepository;
    private final OfferRoomTypeRepository offerRoomTypeRepository;
    private final SeekerRoomTypeRepository seekerRoomTypeRepository;
    private final ChattingRequiredRepository chattingRequiredRepository;
    private final ChattingRoomRepository chattingRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomMessageRepository chatRoomMessageRepository;
    private final RoommateMatchingRequiredRepository roommateMatchingRequiredRepository;
    private final MyRoommateRepository myRoommateRepository;
    private final RoommateHouseRuleRepository roommateHouseRuleRepository;
    private final RoommateCalendarCategoryRepository roommateCalendarCategoryRepository;
    private final RoommateCalendarRepository roommateCalendarRepository;
    private final RoommateCalendarMemberRepository roommateCalendarMemberRepository;
    private final RoommateBoardInterestRepository roommateBoardInterestRepository;
    private final RoommateBoardDeclarationRepository roommateBoardDeclarationRepository;
    private final AlarmSettingRepository alarmSettingRepository;
    private final AlarmRepository alarmRepository;
    private final NotificationRepository notificationRepository;

    private final RoomSeekerProfileRegionRepository roomSeekerProfileRegionRepository;
    private final RoomTypeFileRepository roomTypeFileRepository;
    private final RoomExtraOptionFileRepository roomExtraOptionFileRepository;
    private final LifePatternFileRepository lifePatternFileRepository;
    private final ChatRoomFileRepository chatRoomFileRepository;
    private final MemberLifePatternLogRepository memberLifePatternLogRepository;
    private final PreferenceConditionLogRepository preferenceConditionLogRepository;
    private final PreferenceConditionWeightLogRepository preferenceConditionWeightLogRepository;
    private final RoommateScoreRepository roommateScoreRepository;
    private final NotificationAlarmRepository notificationAlarmRepository;
    private final ChattingRequiredAlarmRepository chattingRequiredAlarmRepository;
    private final RoommateMatchingRequiredAlarmRepository roommateMatchingRequiredAlarmRepository;
    private final RoommateCalendarAlarmRepository roommateCalendarAlarmRepository;
    private final RepeatRoommateCalendarRepository repeatRoommateCalendarRepository;
    private final ExcludeRoommateCalendarRepository excludeRoommateCalendarRepository;
    private final ChattingScoreRepository chattingScoreRepository;
    private final PaymentRepository paymentRepository;
    private final PointRepository pointRepository;
    private final PointLogRepository pointLogRepository;
    private final MemberLifePatternLogDegreeRepository memberLifePatternLogDegreeRepository;
    private final PreferenceConditionLogDegreeRepository preferenceConditionLogDegreeRepository;
    private final PreferenceConditionWeightLogDegreeRepository preferenceConditionWeightLogDegreeRepository;

    @Override
    @Transactional
    @Profile("!prod")
    public void run(String... args) throws Exception {
        if (agreementRepository.count() > 0) {
            return;
        }

        AgreementType agreementType1 = AgreementType.builder().name("서비스 이용약관").build();
        AgreementType agreementType2 = AgreementType.builder().name("개인정보 처리방침").build();
        agreementTypeRepository.saveAll(List.of(agreementType1, agreementType2));

        Agreement termsOfService1 = Agreement.builder().title("서비스 이용약관").contents("상세 내용...").isDeleted(false).isRequired(true).type(agreementType1).build();
        Agreement termsOfService2 = Agreement.builder().title("서비스 이용약관").contents("상세 내용... 수정1").isDeleted(false).isRequired(true).type(agreementType1).build();
        Agreement termsOfService3 = Agreement.builder().title("서비스 이용약관").contents("상세 내용... 수정2").isDeleted(false).isRequired(true).type(agreementType1).build();
        Agreement privacyPolicy = Agreement.builder().title("개인정보 처리방침").contents("상세 내용...").isDeleted(false).isRequired(true).type(agreementType2).build();
        agreementRepository.saveAll(List.of(termsOfService1, termsOfService2, termsOfService3, privacyPolicy));

        AgreementLog termsLog1 = AgreementLog.builder().agreement(termsOfService1).isCurrent(true).build();
        AgreementLog termsLog2 = AgreementLog.builder().agreement(termsOfService2).isCurrent(false).build();
        AgreementLog termsLog3 = AgreementLog.builder().agreement(termsOfService3).isCurrent(false).build();
        AgreementLog privacyLog = AgreementLog.builder().agreement(privacyPolicy).isCurrent(true).build();
        agreementLogRepository.saveAll(List.of(termsLog1, termsLog2, termsLog3, privacyLog));

        LifePattern bedtime = LifePattern.builder().name("취침시간").dtype(LifePatternType.SCALE).lifePatternDescription("평소 취침 시간은 어떤 편인가요?").preferenceDescription("평소 취침 시간은 어떤 편인가요?").isDeleted(false).sort(1).build();
        LifePattern cleanlinessSensitivity = LifePattern.builder().name("청결 민감도").dtype(LifePatternType.SCALE).lifePatternDescription("청결에 얼마나 민감하시나요?").preferenceDescription("청결에 얼마나 민감하시나요?").isDeleted(false).sort(2).build();
        LifePattern noiseSensitivity = LifePattern.builder().name("소음 민감도").dtype(LifePatternType.SCALE).lifePatternDescription("소음에 얼마나 민감하시나요?").preferenceDescription("소음에 얼마나 민감하시나요?").isDeleted(false).sort(3).build();
        LifePattern smoking = LifePattern.builder().name("흡연여부").dtype(LifePatternType.SINGLE_CHOICE).lifePatternDescription("흡연을 하시나요?").preferenceDescription("원하는 룸메이트의 흡연여부를 선택해주세요.").isDeleted(false).sort(4).build();
        LifePattern visitorFrequency = LifePattern.builder().name("방문객 빈도").dtype(LifePatternType.SCALE).lifePatternDescription("주로 얼마나 자주 방문객이 오시나요?").preferenceDescription("원하는 룸메이트의 방문객 빈도를 선택해주세요.").isDeleted(false).sort(5).build();
        LifePattern pet = LifePattern.builder().name("반려동물 여부").dtype(LifePatternType.SINGLE_CHOICE).lifePatternDescription("반려동물을 키우고 있나요?").preferenceDescription("원하는 룸메이트의 반려동물 여부를 선택해주세요.").isDeleted(false).sort(6).build();
        LifePattern personalSpaceImportance = LifePattern.builder().name("개인 공간 중요도").dtype(LifePatternType.SCALE).lifePatternDescription("개인 공간을 얼마나 중요하게 생각하시나요?").preferenceDescription("원하는 룸메이트의 개인 공간 중요도를 선택해주세요.").isDeleted(false).sort(7).build();
        LifePattern personalityStyle = LifePattern.builder().name("성격 스타일").dtype(LifePatternType.SCALE).lifePatternDescription("나의 성격은 어떤 편인가요?").preferenceDescription("원하는 룸메이트의 성격을 선택해주세요.").isDeleted(false).sort(8).build();
        lifePatternRepository.saveAll(List.of(
                bedtime,
                visitorFrequency,
                smoking,
                pet,
                cleanlinessSensitivity,
                noiseSensitivity,
                personalSpaceImportance,
                personalityStyle
        ));

        List<LifePatternInformation> lifePatternInformationList = new ArrayList<>();
        lifePatternInformationList.addAll(createLifePatternInformation(bedtime,
                "매우 일찍", "일찍", "보통", "늦게", "매우 늦게"));
        lifePatternInformationList.addAll(createLifePatternInformation(visitorFrequency,
                "거의 안 옴", "가끔", "보통", "자주", "매우 자주"));
        lifePatternInformationList.addAll(createLifePatternInformation(smoking,
                "하지 않아요", "하고 있어요"));
        lifePatternInformationList.addAll(createLifePatternInformation(pet,
                "키우지 않아요", "키우고 있어요"));
        lifePatternInformationList.addAll(createLifePatternInformation(cleanlinessSensitivity,
                "전혀 민감하지 않음", "민감하지 않음", "보통", "민감함", "매우 민감함"));
        lifePatternInformationList.addAll(createLifePatternInformation(noiseSensitivity,
                "전혀 민감하지 않음", "민감하지 않음", "보통", "민감함", "매우 민감함"));
        lifePatternInformationList.addAll(createLifePatternInformation(personalSpaceImportance,
                "전혀 중요하지 않음", "중요하지 않음", "보통", "중요함", "매우 중요함"));
        lifePatternInformationList.addAll(createLifePatternInformation(personalityStyle,
                "매우 내향적", "내향적", "보통", "외향적", "매우 외향적"));
        lifePatternInformationRepository.saveAll(lifePatternInformationList);

        Region seoul = Region.builder().name("서울특별시").scope(1).parent(null).build();
        Region gyeonggi = Region.builder().name("경기도").scope(1).parent(null).build();
        regionRepository.saveAll(List.of(seoul, gyeonggi));

        List<Region> level2Districts = new ArrayList<>();
        List<Region> level3Towns = new ArrayList<>();

        String[] seoulGuList = {"강남구", "마포구", "송파구", "서초구", "성동구", "종로구", "영등포구", "용산구"};
        String[][] seoulDongList = {
                {"역삼동", "삼성동", "청담동", "논현동"},
                {"서교동", "합정동", "망원동", "연남동"},
                {"잠실동", "문정동", "가락동", "방이동"},
                {"반포동", "방배동", "서초동", "양재동"},
                {"성수동", "옥수동", "왕십리동", "마장동"},
                {"혜화동", "명륜동", "삼청동", "평창동"},
                {"여의도동", "당산동", "문래동", "신길동"},
                {"이태원동", "한남동", "이촌동", "후암동"}
        };

        for (int i = 0; i < seoulGuList.length; i++) {
            Region gu = Region.builder().name(seoulGuList[i]).scope(2).parent(seoul).build();
            level2Districts.add(gu);
        }

        String[] gyeonggiSiList = {"수원시 영통구", "성남시 분당구", "고양시 일산동구", "용인시 수지구", "안양시 동안구", "부천시", "남양주시", "화성시"};
        String[][] gyeonggiDongList = {
                {"영통동", "망포동", "매탄동", "이의동"},
                {"삼평동", "서현동", "정자동", "야탑동"},
                {"장항동", "마두동", "백석동", "식사동"},
                {"풍덕천동", "죽전동", "동천동", "상현동"},
                {"범계동", "평촌동", "관양동", "호계동"},
                {"중동", "상동", "심곡동", "소사본동"},
                {"다산동", "별내동", "와부읍", "진접읍"},
                {"동탄동", "향남읍", "봉담읍", "새솔동"}
        };

        for (int i = 0; i < gyeonggiSiList.length; i++) {
            Region si = Region.builder().name(gyeonggiSiList[i]).scope(2).parent(gyeonggi).build();
            level2Districts.add(si);
        }
        regionRepository.saveAll(level2Districts);

        for (int i = 0; i < seoulGuList.length; i++) {
            Region parentGu = level2Districts.get(i);
            for (int j = 0; j < 4; j++) {
                level3Towns.add(Region.builder().name(seoulDongList[i][j]).scope(3).parent(parentGu).build());
            }
        }
        for (int i = 0; i < gyeonggiSiList.length; i++) {
            Region parentSi = level2Districts.get(i + seoulGuList.length);
            for (int j = 0; j < 4; j++) {
                level3Towns.add(Region.builder().name(gyeonggiDongList[i][j]).scope(3).parent(parentSi).build());
            }
        }
        regionRepository.saveAll(level3Towns);

        RoomType oneRoom = RoomType.builder().name("원룸").isDeleted(false).build();
        RoomType twoRoom = RoomType.builder().name("투룸").isDeleted(false).build();
        RoomType threeRoomPlus = RoomType.builder().name("쓰리룸+").isDeleted(false).build();
        RoomType officetel = RoomType.builder().name("오피스텔").isDeleted(false).build();
        RoomType apartment = RoomType.builder().name("아파트").isDeleted(false).build();
        roomTypeRepository.saveAll(List.of(oneRoom, twoRoom, threeRoomPlus, officetel, apartment));

        RoomExtraOption fullOption = RoomExtraOption.builder().name("풀옵션").isDeleted(false).build();
        RoomExtraOption elevator = RoomExtraOption.builder().name("엘레베이터").isDeleted(false).build();
        RoomExtraOption parking = RoomExtraOption.builder().name("주차가능").isDeleted(false).build();
        RoomExtraOption veranda = RoomExtraOption.builder().name("베란다/발코니").isDeleted(false).build();
        RoomExtraOption petAvailable = RoomExtraOption.builder().name("반려동물 협의").isDeleted(false).build();
        RoomExtraOption securityCctv = RoomExtraOption.builder().name("보안/CCTV").isDeleted(false).build();

        roomExtraOptionRepository.saveAll(List.of(fullOption, elevator, parking, veranda, petAvailable, securityCctv));

        InquiryCategory catAccount = InquiryCategory.builder().title("계정/인증").isDeleted(false).build();
        InquiryCategory catRoom = InquiryCategory.builder().title("방 등록/매칭").isDeleted(false).build();
        InquiryCategory catAbuse = InquiryCategory.builder().title("불량유저 신고").isDeleted(false).build();
        InquiryCategory catEtc = InquiryCategory.builder().title("기타 문의").isDeleted(false).build();
        InquiryCategory feedBack = InquiryCategory.builder().title("의견 남기기").isDeleted(false).build();

        inquiryCategoryRepository.saveAll(List.of(catAccount, catRoom, catAbuse, catEtc, feedBack));

        // ==================== 전수 JPA 엔티티 E2E 덤프 데이터 생성 ====================

        // 1. 앱 버전 (AppVersion)
        AppVersion androidVersion = AppVersion.builder()
                .platformType(PlatformType.ANDROID)
                .version("1.0.0")
                .minVersion("1.0.0")
                .updateType(UpdateType.SELECT)
                .isDeleted(false)
                .build();
        AppVersion iosVersion = AppVersion.builder()
                .platformType(PlatformType.IOS)
                .version("1.0.0")
                .minVersion("1.0.0")
                .updateType(UpdateType.SELECT)
                .isDeleted(false)
                .build();
        appVersionRepository.saveAll(List.of(androidVersion, iosVersion));

        // 2. 인증 이메일 도메인 (AuthEmail)
        AuthEmail univEmail = AuthEmail.builder()
                .domain("univ.ac.kr")
                .name("대학교 공통")
                .dtype(AuthenticationType.STUDENT)
                .isDeleted(false)
                .build();
        AuthEmail companyEmail = AuthEmail.builder()
                .domain("company.com")
                .name("일반 기업")
                .dtype(AuthenticationType.COMPANY)
                .isDeleted(false)
                .build();
        authEmailRepository.saveAll(List.of(univEmail, companyEmail));

        // 3. 회원 (Member) 및 기본정보 (BasicInformation) & 회원 상태 (State) & 공개 설정 (MemberPrivacy)
        Member adminMember = Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId("admin_provider_123")
                .role(MemberRole.ADMIN)
                .isDelete(false)
                .build();
        Member user1 = Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId("user1_provider_456")
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        Member user2 = Member.builder()
                .providerType(LoginProviderType.APPLE)
                .providerId("user2_provider_789")
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        Member user3 = Member.builder()
                .providerType(LoginProviderType.APPLE)
                .providerId("user2_provider_886")
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        memberRepository.saveAll(List.of(adminMember, user1, user2, user3));

        stateRepository.saveAll(List.of(
                State.builder().member(adminMember).states(MemberState.ACTIVE).build(),
                State.builder().member(user1).states(MemberState.ACTIVE).build(),
                State.builder().member(user2).states(MemberState.ACTIVE).build()
        ));

        memberPrivacyRepository.saveAll(List.of(
                MemberPrivacy.builder().member(user1).type(MemberPrivacyType.PUBLIC).build(),
                MemberPrivacy.builder().member(user2).type(MemberPrivacyType.PUBLIC).build()
        ));

        BasicInformation adminInfo = BasicInformation.builder()
                .member(adminMember)
                .name("관리자")
                .birth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .email("admin@knockin.com")
                .build();
        BasicInformation user1Info = BasicInformation.builder()
                .member(user1)
                .name("김노크")
                .birth(LocalDate.of(2000, 5, 15))
                .gender(Gender.FEMALE)
                .email("user1@univ.ac.kr")
                .build();
        BasicInformation user2Info = BasicInformation.builder()
                .member(user2)
                .name("이인인")
                .birth(LocalDate.of(1998, 8, 20))
                .gender(Gender.MALE)
                .email("user2@company.com")
                .build();
        basicInformationRepository.saveAll(List.of(adminInfo, user1Info, user2Info));

        // 4. 회원 약관 동의 (MemberAgreement)
        memberAgreementRepository.saveAll(List.of(
                MemberAgreement.builder().memberId(user1.getId()).agreementLog(termsLog1).isAgreed(true).build(),
                MemberAgreement.builder().memberId(user1.getId()).agreementLog(privacyLog).isAgreed(true).build(),
                MemberAgreement.builder().memberId(user2.getId()).agreementLog(termsLog1).isAgreed(true).build(),
                MemberAgreement.builder().memberId(user2.getId()).agreementLog(privacyLog).isAgreed(true).build()
        ));

        // 5. 회원 생활패턴 (MemberLifePattern) & 선호조건 (PreferenceCondition & Weight)
        if (!lifePatternInformationList.isEmpty()) {
            memberLifePatternRepository.saveAll(List.of(
                    MemberLifePattern.builder().member(user1).lifePatternInformation(lifePatternInformationList.get(0)).build(),
                    MemberLifePattern.builder().member(user2).lifePatternInformation(lifePatternInformationList.get(0)).build()
            ));

            preferenceConditionRepository.saveAll(List.of(
                    PreferenceCondition.builder().member(user1).lifePatternInformation(lifePatternInformationList.get(0)).build(),
                    PreferenceCondition.builder().member(user2).lifePatternInformation(lifePatternInformationList.get(0)).build()
            ));

            preferenceConditionWeightRepository.saveAll(List.of(
                    PreferenceConditionWeight.builder().member(user1).lifePattern(bedtime).build(),
                    PreferenceConditionWeight.builder().member(user2).lifePattern(cleanlinessSensitivity).build()
            ));
        }

        // 6. 회원 관심 상대 (MemberInterest) & 차단 (Block) & 회원 신고 (MemberDeclaration)
        memberInterestRepository.save(MemberInterest.builder().sender(user1).receiver(user2).isDeleted(false).build());
        blockRepository.save(Block.builder().blocker(user1).blocked(user2).isDeleted(false).build());
        memberDeclarationRepository.save(MemberDeclaration.builder().reporterId(user1.getId()).reportedId(user2.getId()).reason("불쾌한 채팅 메세지").declarationType(DeclarationType.PENDING).build());

        // 7. 자주 묻는 질문 (FAQ)
        Faq faq1 = Faq.builder()
                .title("룸메이트 매칭 서비스 이용 방법")
                .contents("룸메이트 게시글을 작성하고 원하는 조건의 룸메이트와 채팅을 신청해보세요.")
                .member(adminMember)
                .sort(1)
                .isDeleted(false)
                .build();
        Faq faq2 = Faq.builder()
                .title("신원 인증은 어떻게 하나요?")
                .contents("학생증 또는 재직증명서를 첨부하여 인증 신청 시 관리자 확인 후 인증이 완료됩니다.")
                .member(adminMember)
                .sort(2)
                .isDeleted(false)
                .build();
        faqRepository.saveAll(List.of(faq1, faq2));

        // 8. 학생/직장인 신원 인증 (Authentication & AuthenticationApprove)
        Authentication authUser1 = Authentication.builder()
                .memberId(user1.getId())
                .type(AuthenticationType.STUDENT)
                .email("user1@univ.ac.kr")
                .code("123456")
                .isAccepted(true)
                .build();
        Authentication authUser2 = Authentication.builder()
                .memberId(user2.getId())
                .type(AuthenticationType.COMPANY)
                .email("user2@company.com")
                .code("654321")
                .isAccepted(true)
                .build();
        authenticationRepository.saveAll(List.of(authUser1, authUser2));

        AuthenticationApprove approveUser1 = AuthenticationApprove.builder()
                .authentication(authUser1)
                .status(ApproveType.ACCEPTED)
                .build();
        AuthenticationApprove approveUser2 = AuthenticationApprove.builder()
                .authentication(authUser2)
                .status(ApproveType.ACCEPTED)
                .build();
        authenticationApproveRepository.saveAll(List.of(approveUser1, approveUser2));

        // 9. 파일 메타데이터 (File) & 프로필 사진 (BasicInformationFile)
        File profilePic = File.builder()
                .type(FileType.USER_PROFILE_IMAGE)
                .originalFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .savedFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        File boardPic = File.builder()
                .type(FileType.ROOMMATE_BOARD_IMAGE)
                .originalFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .savedFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        File roomTypePic = File.builder()
                .type(FileType.ETC)
                .originalFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .savedFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        File extraOptionPic = File.builder()
                .type(FileType.ETC)
                .originalFileName("https://velog.velcdn.com/images/sdb016/post/47181c7c-1156-4182-a638-e0ad0b03a3d3/test")
                .savedFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        File lifePatternPic = File.builder()
                .type(FileType.ETC)
                .originalFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .savedFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        File chatRoomPic = File.builder()
                .type(FileType.CHAT_ROOM_IMAGE)
                .originalFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .savedFileName("https://i.pinimg.com/736x/81/58/be/8158be42360c6264c1688d300a0fb8b0.jpg")
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        fileRepository.saveAll(List.of(profilePic, boardPic, roomTypePic, extraOptionPic, lifePatternPic, chatRoomPic));

        basicInformationFileRepository.save(BasicInformationFile.builder().basicInformation(user1Info).file(profilePic).build());

        roomTypeFileRepository.save(RoomTypeFile.builder().roomType(oneRoom).file(roomTypePic).build());
        roomExtraOptionFileRepository.save(RoomExtraOptionFile.builder().roomExtraOption(fullOption).file(extraOptionPic).build());
        lifePatternFileRepository.save(LifePatternFile.builder().lifePattern(bedtime).file(lifePatternPic).build());

        // 10. 방 프로필 (RoomOfferProfile, RoomSeekerProfile, OfferRoomType, SeekerRoomType, SeekerProfileRegion)
        RoomOfferProfile offerProfile = RoomOfferProfile.builder()
                .member(user1)
                .region(seoul)
                .deposit(500)
                .monthlyRent(50)
                .comeableAt(LocalDateTime.now().plusDays(7))
                .isComeableAtNegotiable(true)
                .build();
        RoomSeekerProfile seekerProfile = RoomSeekerProfile.builder()
                .member(user2)
                .minDeposit(100)
                .maxDeposit(1000)
                .minMonthlyRent(30)
                .maxMonthlyRent(80)
                .comeableAt(LocalDateTime.now().plusDays(7))
                .isComeableAtNegotiable(true)
                .build();
        roomOfferProfileRepository.save(offerProfile);
        roomSeekerProfileRepository.save(seekerProfile);

        offerRoomTypeRepository.save(OfferRoomType.builder().roomOfferProfile(offerProfile).roomType(oneRoom).build());
        seekerRoomTypeRepository.save(SeekerRoomType.builder().roomSeekerProfile(seekerProfile).roomType(oneRoom).build());
        roomSeekerProfileRegionRepository.save(RoomSeekerProfileRegion.builder().roomSeekerProfile(seekerProfile).region(seoul).build());

        // 11. 인기 검색어 덤프 (Search)
        Search search1 = Search.builder().member(user1).keyword("강남역").build();
        Search search2 = Search.builder().member(user1).keyword("원룸").build();
        Search search3 = Search.builder().member(user2).keyword("강남역").build();
        Search search4 = Search.builder().member(user2).keyword("판교").build();
        Search search5 = Search.builder().member(user2).keyword("역삼동").build();
        searchRepository.saveAll(List.of(search1, search2, search3, search4, search5));

        // 12. 룸메이트 게시글, 옵션 및 첨부파일 (RoommateBoard, Option, File, Interest, Declaration)
        Region firstDong = level3Towns.isEmpty() ? seoul : level3Towns.get(0);
        RoommateBoard board1 = RoommateBoard.builder()
                .member(user1)
                .title("강남역 인근 깔끔한 원룸 룸메 구합니다!")
                .contents("채광 좋고 조용한 원룸입니다. 함께 깨끗하게 쓰실 분 환영해요.")
                .deposit(500)
                .monthlyRent(50)
                .managementCost(5)
                .roomType(oneRoom)
                .region(firstDong)
                .comeableDateNegotiable(true)
                .comeableDate(LocalDateTime.now().plusDays(7))
                .isDeleted(false)
                .hits(10L)
                .build();
        roommateBoardRepository.save(board1);

        RoommateBoardOption boardOption1 = RoommateBoardOption.builder()
                .roommateBoard(board1)
                .roomExtraOption(fullOption)
                .build();
        RoommateBoardOption boardOption2 = RoommateBoardOption.builder()
                .roommateBoard(board1)
                .roomExtraOption(elevator)
                .build();
        roommateBoardOptionRepository.saveAll(List.of(boardOption1, boardOption2));

        roommateBoardFileRepository.save(RoommateBoardFile.builder().roommateBoard(board1).file(boardPic).isThumbnail(true).build());
        roommateBoardInterestRepository.save(RoommateBoardInterest.builder().member(user2).roommateBoard(board1).isDeleted(false).build());
        roommateBoardDeclarationRepository.save(RoommateBoardDeclaration.builder().memberId(user2.getId()).roommateBoardId(board1.getId()).reason("허위 매물 등록 의심").declarationType(DeclarationType.PENDING).build());

        // 12-1. 추가 테스트용
        RoommateBoard board2 = RoommateBoard.builder()
                .member(user1)
                .title("강남역 인근 깔끔한 원룸 룸메 구합니다!")
                .contents("채광 좋고 조용한 원룸입니다. 함께 깨끗하게 쓰실 분 환영해요.")
                .deposit(500)
                .monthlyRent(50)
                .managementCost(5)
                .roomType(oneRoom)
                .region(firstDong)
                .comeableDateNegotiable(true)
                .comeableDate(LocalDateTime.now().plusDays(7))
                .isDeleted(false)
                .hits(10L)
                .build();
        roommateBoardRepository.save(board2);

        RoommateBoardOption boardOption3 = RoommateBoardOption.builder()
                .roommateBoard(board2)
                .roomExtraOption(fullOption)
                .build();
        RoommateBoardOption boardOption4 = RoommateBoardOption.builder()
                .roommateBoard(board2)
                .roomExtraOption(elevator)
                .build();

        roommateBoardOptionRepository.saveAll(List.of(boardOption3, boardOption4));
        roommateBoardDeclarationRepository.save(RoommateBoardDeclaration.builder().memberId(user2.getId()).roommateBoardId(board2.getId()).reason("허위 매물 등록 의심").declarationType(DeclarationType.PENDING).build());

        // 13. 1:1 문의 및 답변 (Inquiry & InquiryComment)
        Inquiry inquiry1 = Inquiry.builder()
                .memberId(user1.getId())
                .inquiryCategory(catAccount)
                .title("신원 인증 처리 기간이 궁금합니다.")
                .contents("학생 인증 신청을 했는데 승인까지 보통 얼마나 걸리나요?")
                .isDeleted(false)
                .build();
        inquiryRepository.save(inquiry1);

        InquiryComment comment1 = InquiryComment.builder()
                .memberId(adminMember.getId())
                .inquiry(inquiry1)
                .contents("안녕하세요 김노크님! 신원 인증은 제출 후 영업일 기준 1~2일 이내에 검토 완료됩니다.")
                .isDeleted(false)
                .build();
        inquiryCommentRepository.save(comment1);

        // 14. 채팅 신청, 채팅방, 채팅 멤버 및 채팅 메세지, 채팅방 이미지 (ChattingRequired, ChattingRoom, Member, Message, ChatRoomFile)
        ChattingRequired chatReq = ChattingRequired.builder()
                .requester(user2)
                .requestee(user1)
                .roommateBoard(board1)
                .status(ChattingRequiredStatus.ACCEPTED)
                .build();
        chattingRequiredRepository.save(chatReq);

        ChattingRoom chatRoom = ChattingRoom.builder()
                .chattingRequired(chatReq)
                .build();
        chattingRoomRepository.save(chatRoom);

        ChatRoomMember mem1 = ChatRoomMember.of(chatRoom, user1);
        ChatRoomMember mem2 = ChatRoomMember.of(chatRoom, user2);
        chatRoomMemberRepository.saveAll(List.of(mem1, mem2));

        ChatRoomMessage msg1 = ChatRoomMessage.builder()
                .chattingRoom(chatRoom)
                .member(user2)
                .contents("안녕하세요! 강남역 게시글 보고 문의드립니다.")
                .type(MessageType.TEXT)
                .isRead(true)
                .build();
        ChatRoomMessage msg2 = ChatRoomMessage.builder()
                .chattingRoom(chatRoom)
                .member(user1)
                .contents("네 안녕하세요! 언제 보러 오시겠어요?")
                .type(MessageType.TEXT)
                .isRead(false)
                .build();
        chatRoomMessageRepository.saveAll(List.of(msg1, msg2));

        // 15. 이력 로그 및 궁합 점수 (MemberLifePatternLog, PreferenceConditionLog, WeightLog)
        PreferenceConditionLogDegree preferenceConditionLogDegree = preferenceConditionLogDegreeRepository.save(
                PreferenceConditionLogDegree.builder().degree(1L).build());
        MemberLifePatternLogDegree memberLifePatternLogDegree = memberLifePatternLogDegreeRepository.save(
                MemberLifePatternLogDegree.builder().degree(1L).build());
        PreferenceConditionWeightLogDegree preferenceConditionWeightLogDegree = preferenceConditionWeightLogDegreeRepository.save(PreferenceConditionWeightLogDegree.builder().degree(1L).build());
        MemberLifePatternLog mlpLog = memberLifePatternLogRepository.save(
                MemberLifePatternLog.builder().member(user1).lifePatternInformation(lifePatternInformationList.get(0)).memberLifePatternLogDegree(memberLifePatternLogDegree).build());
        PreferenceConditionLog pcLog = preferenceConditionLogRepository.save(
                PreferenceConditionLog.builder().member(user1).lifePatternInformation(lifePatternInformationList.get(0)).preferenceConditionLogDegree(preferenceConditionLogDegree).build());
        PreferenceConditionWeightLog pcwLog = preferenceConditionWeightLogRepository.save(
                PreferenceConditionWeightLog.builder().member(user1).lifePattern(bedtime).preferenceConditionWeightLogDegree(preferenceConditionWeightLogDegree).build());

        chatRoomFileRepository.save(ChatRoomFile.builder().chatRoomMessage(msg1).file(chatRoomPic).build());
        ChattingScore chattingScore = chattingScoreRepository.save(ChattingScore.builder().chattingRequired(chatReq).memberLifePatternLogDegree(memberLifePatternLogDegree).preferenceConditionLogDegree(preferenceConditionLogDegree).preferenceConditionWeightLogDegree(preferenceConditionWeightLogDegree).score(90).build());

        // 16. 룸메이트 매칭 확정 및 내 룸메이트 (RoommateMatchingRequired, MyRoommate)
        RoommateMatchingRequired matchingReq = RoommateMatchingRequired.builder()
                .requester(user2)
                .requestee(user1)
                .chattingRoom(chatRoom)
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();
        roommateMatchingRequiredRepository.save(matchingReq);

        MyRoommate myRoommate = MyRoommate.builder()
                .roommateMatchingRequired(matchingReq)
                .isDeleted(false)
                .build();
        myRoommateRepository.save(myRoommate);

        roommateScoreRepository.save(RoommateScore.builder()
                .myRoommate(myRoommate)
                .chattingScore(chattingScore)
                .build());

        // 17. 룸메이트 하우스 룰 (RoommateHouseRule)
        RoommateHouseRule rule1 = RoommateHouseRule.builder()
                .member(user1)
                .myRoommate(myRoommate)
                .title("청소 규칙")
                .contents("화장실 청소는 매주 토요일 교대로 진행합니다.")
                .isDeleted(false)
                .build();
        RoommateHouseRule rule2 = RoommateHouseRule.builder()
                .member(user1)
                .myRoommate(myRoommate)
                .title("소음 지정")
                .contents("밤 11시 이후 이어폰 착용 필수!")
                .isDeleted(false)
                .build();
        roommateHouseRuleRepository.saveAll(List.of(rule1, rule2));

        // 18. 룸메이트 캘린더 카테고리, 캘린더, 캘린더 참여자, 반복 및 예외 (CalendarCategory, Calendar, Member, Repeat, Exclude)
        RoommateCalendarCategory calCat = RoommateCalendarCategory.builder()
                .name("집안일")
                .build();
        roommateCalendarCategoryRepository.save(calCat);

        RoommateCalendar calendar = RoommateCalendar.builder()
                .myRoommate(myRoommate)
                .member(user1)
                .roommateCalendarCategory(calCat)
                .title("거실 및 주방 청소")
                .contents("주말 맞이 대청소")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(2))
                .isDeleted(false)
                .build();
        roommateCalendarRepository.save(calendar);

        RoommateCalendarMember calMem1 = RoommateCalendarMember.of(calendar, user1);
        RoommateCalendarMember calMem2 = RoommateCalendarMember.of(calendar, user2);
        roommateCalendarMemberRepository.saveAll(List.of(calMem1, calMem2));

        RepeatRoommateCalendar repeatCal = repeatRoommateCalendarRepository.save(RepeatRoommateCalendar.builder()
                .roommateCalendar(calendar)
                .repeatType(RepeatType.WEEKLY)
                .endDate(LocalDateTime.now().plusMonths(3))
                .build());

        excludeRoommateCalendarRepository.save(ExcludeRoommateCalendar.builder()
                .repeatRoommateCalendar(repeatCal)
                .excludeAt(LocalDateTime.now().plusDays(7))
                .build());

        // 19. 알림 설정, 푸시 알림, 연관 알림 및 공지사항 (AlarmSetting, Alarm, Notification, NotificationAlarm, ChattingRequiredAlarm, MatchingRequiredAlarm, CalendarAlarm)
        AlarmSetting setting1 = AlarmSetting.builder()
                .member(user1)
                .alarmSettingType(AlarmSettingType.NOTIFICATION)
                .isEnabled(true)
                .build();
        alarmSettingRepository.save(setting1);

        Alarm alarm1 = Alarm.builder()
                .member(user1)
                .title("채팅 알림")
                .contents("이인인 님으로부터 새로운 메시지가 도착했습니다.")
                .expiredAt(LocalDateTime.now().plusDays(30))
                .isRead(false)
                .build();
        alarmRepository.save(alarm1);

        Notification notice1 = Notification.builder()
                .member(adminMember)
                .title("서비스 정기 점검 안내")
                .contents("안정적인 서비스 환경 조성을 위한 시스템 정기 점검이 수행됩니다.")
                .isDeleted(false)
                .build();
        notificationRepository.save(notice1);

        notificationAlarmRepository.save(NotificationAlarm.builder().alarm(alarm1).notification(notice1).build());
        chattingRequiredAlarmRepository.save(ChattingRequiredAlarm.builder().member(user1).title("채팅 신청 알림").expiredAt(LocalDateTime.now().plusDays(30)).chattingRequired(chatReq).build());
        roommateMatchingRequiredAlarmRepository.save(RoommateMatchingRequiredAlarm.builder().member(user1).title("매칭 확정 알림").expiredAt(LocalDateTime.now().plusDays(30)).roommateMatchingRequired(matchingReq).build());
        roommateCalendarAlarmRepository.save(RoommateCalendarAlarm.builder().member(user1).title("일정 알림").expiredAt(LocalDateTime.now().plusDays(30)).roommateCalendar(calendar).build());

        // 20. 결제, 포인트 및 포인트 거래이력 (Payment, Point, PointLog)
        Payment payment = Payment.builder()
                .member(user1)
                .paymentKey("payment_key_sample_123")
                .amount(10000L)
                .type(PaymentType.CARD)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        Point point = Point.builder()
                .member(user1)
                .points(1000L)
                .build();
        pointRepository.save(point);

        PointLog pointLog = PointLog.builder()
                .member(user1)
                .points(1000L)
                .reason("신규 가입 리워드 포인트 지급")
                .variance(VarianceType.INCREASE)
                .build();
        pointLogRepository.save(pointLog);

        seedLoadTestData(
                loadTestRecordsPerEntity,
                level3Towns,
                List.of(oneRoom, twoRoom, threeRoomPlus, officetel, apartment),
                fullOption,
                elevator,
                user2,
                adminMember,
                catAccount,
                profilePic,
                boardPic,
                chatRoomPic
        );
    }

    private void seedLoadTestData(
            int recordCount,
            List<Region> regions,
            List<RoomType> roomTypes,
            RoomExtraOption firstOption,
            RoomExtraOption secondOption,
            Member supportMember,
            Member adminMember,
            InquiryCategory inquiryCategory,
            File profileFile,
            File boardFile,
            File chatFile
    ) {
        if (recordCount <= 0) {
            return;
        }

        List<Member> members = new ArrayList<>(recordCount);
        for (int i = 1; i <= recordCount; i++) {
            members.add(Member.builder()
                    .providerType(i % 2 == 0 ? LoginProviderType.KAKAO : LoginProviderType.APPLE)
                    .providerId("load_test_user_" + i)
                    .role(MemberRole.USER)
                    .isDelete(false)
                    .build());
        }
        memberRepository.saveAll(members);

        List<State> states = new ArrayList<>(recordCount);
        List<BasicInformation> basicInformation = new ArrayList<>(recordCount);
        List<MemberPrivacy> memberPrivacies = new ArrayList<>(recordCount);
        List<AgreementType> agreementTypes = new ArrayList<>(recordCount);
        List<Agreement> agreements = new ArrayList<>(recordCount);
        List<AgreementLog> agreementLogs = new ArrayList<>(recordCount);
        List<MemberAgreement> memberAgreements = new ArrayList<>(recordCount);
        List<RoommateBoard> boards = new ArrayList<>(recordCount);
        List<RoommateBoardOption> boardOptions = new ArrayList<>(recordCount * 2);
        List<RoommateBoardFile> boardFiles = new ArrayList<>(recordCount);
        List<RoommateBoardInterest> boardInterests = new ArrayList<>(recordCount);
        List<Search> searches = new ArrayList<>(recordCount);
        List<Inquiry> inquiries = new ArrayList<>(recordCount);
        List<InquiryComment> inquiryComments = new ArrayList<>(recordCount);
        List<ChattingRequired> chattingRequirements = new ArrayList<>(recordCount);
        List<ChattingRoom> chattingRooms = new ArrayList<>(recordCount);
        List<ChatRoomMember> chatRoomMembers = new ArrayList<>(recordCount * 2);
        List<ChatRoomMessage> chatMessages = new ArrayList<>(recordCount * 2);
        List<ChatRoomFile> chatRoomFiles = new ArrayList<>(recordCount);
        List<Payment> payments = new ArrayList<>(recordCount);
        List<Point> points = new ArrayList<>(recordCount);
        List<PointLog> pointLogs = new ArrayList<>(recordCount);

        for (int i = 0; i < recordCount; i++) {
            Member member = members.get(i);
            states.add(State.builder().member(member).states(MemberState.ACTIVE).build());
            basicInformation.add(BasicInformation.builder()
                    .member(member)
                    .name("테스터" + ((i + 1) % 100000))
                    .birth(LocalDate.of(1990 + i % 20, i % 12 + 1, i % 28 + 1))
                    .gender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                    .email("loadtest" + (i + 1) + "@example.com")
                    .build());
            memberPrivacies.add(MemberPrivacy.builder().member(member).type(MemberPrivacyType.PUBLIC).build());

            AgreementType agreementType = AgreementType.builder()
                    .name("부하테스트 약관 유형 " + (i + 1))
                    .isDeleted(false)
                    .build();
            agreementTypes.add(agreementType);
            Agreement agreement = Agreement.builder()
                    .type(agreementType)
                    .title("부하테스트 약관 " + (i + 1))
                    .contents("nGrinder 부하 테스트용 약관 데이터입니다. " + (i + 1))
                    .isDeleted(false)
                    .isRequired(true)
                    .build();
            agreements.add(agreement);
            agreementLogs.add(AgreementLog.builder().agreement(agreement).isCurrent(true).build());
            memberAgreements.add(MemberAgreement.builder().memberId(member.getId()).agreementLog(agreementLogs.get(i)).isAgreed(true).build());

            RoommateBoard board = RoommateBoard.builder()
                    .member(member)
                    .title("부하 테스트 게시글 " + (i + 1))
                    .contents("nGrinder 목록 및 검색 부하 테스트용으로 생성된 게시글입니다. " + (i + 1))
                    .deposit(300 + i % 20 * 50)
                    .monthlyRent(30 + i % 10 * 5)
                    .managementCost(5 + i % 5)
                    .roomType(roomTypes.get(i % roomTypes.size()))
                    .region(regions.get(i % regions.size()))
                    .comeableDateNegotiable(i % 2 == 0)
                    .comeableDate(LocalDateTime.now().plusDays(i % 90 + 1))
                    .isDeleted(false)
                    .hits((long) (i % 1000))
                    .build();
            boards.add(board);
            boardOptions.add(RoommateBoardOption.builder().roommateBoard(board).roomExtraOption(firstOption).build());
            boardOptions.add(RoommateBoardOption.builder().roommateBoard(board).roomExtraOption(secondOption).build());
            boardFiles.add(RoommateBoardFile.builder().roommateBoard(board).file(boardFile).isThumbnail(true).build());
            boardInterests.add(RoommateBoardInterest.builder().member(supportMember).roommateBoard(board).isDeleted(false).build());
            searches.add(Search.builder().member(member).keyword("부하테스트 " + (i + 1)).build());

            Inquiry inquiry = Inquiry.builder()
                    .memberId(member.getId())
                    .inquiryCategory(inquiryCategory)
                    .title("부하 테스트 문의 " + (i + 1))
                    .contents("nGrinder 문의 목록 테스트용 데이터입니다. " + (i + 1))
                    .isDeleted(false)
                    .build();
            inquiries.add(inquiry);
            inquiryComments.add(InquiryComment.builder()
                    .memberId(adminMember.getId())
                    .inquiry(inquiry)
                    .contents("부하 테스트 답변 " + (i + 1))
                    .isDeleted(false)
                    .build());

            ChattingRequired chattingRequired = ChattingRequired.builder()
                    .requester(member)
                    .requestee(supportMember)
                    .roommateBoard(board)
                    .status(ChattingRequiredStatus.ACCEPTED)
                    .build();
            chattingRequirements.add(chattingRequired);
            ChattingRoom chattingRoom = ChattingRoom.builder().chattingRequired(chattingRequired).build();
            chattingRooms.add(chattingRoom);
            chatRoomMembers.add(ChatRoomMember.of(chattingRoom, member));
            chatRoomMembers.add(ChatRoomMember.of(chattingRoom, supportMember));
            ChatRoomMessage firstMessage = ChatRoomMessage.builder()
                    .chattingRoom(chattingRoom)
                    .member(member)
                    .contents("부하 테스트 메시지 " + (i + 1) + "-1")
                    .type(MessageType.TEXT)
                    .isRead(true)
                    .build();
            ChatRoomMessage secondMessage = ChatRoomMessage.builder()
                    .chattingRoom(chattingRoom)
                    .member(supportMember)
                    .contents("부하 테스트 메시지 " + (i + 1) + "-2")
                    .type(MessageType.TEXT)
                    .isRead(false)
                    .build();
            chatMessages.add(firstMessage);
            chatMessages.add(secondMessage);
            chatRoomFiles.add(ChatRoomFile.builder().chatRoomMessage(firstMessage).file(chatFile).build());

            payments.add(Payment.builder()
                    .member(member)
                    .paymentKey("load_test_payment_" + (i + 1))
                    .amount(10000L + i)
                    .type(PaymentType.CARD)
                    .status(PaymentStatus.PENDING)
                    .build());
            points.add(Point.builder().member(member).points(1000L + i).build());
            pointLogs.add(PointLog.builder()
                    .member(member)
                    .points(1000L + i)
                    .reason("부하 테스트 포인트 " + (i + 1))
                    .variance(VarianceType.INCREASE)
                    .build());
        }

        agreementTypeRepository.saveAll(agreementTypes);
        agreementRepository.saveAll(agreements);
        agreementLogRepository.saveAll(agreementLogs);
        stateRepository.saveAll(states);
        basicInformationRepository.saveAll(basicInformation);
        memberPrivacyRepository.saveAll(memberPrivacies);
        memberAgreementRepository.saveAll(memberAgreements);
        roommateBoardRepository.saveAll(boards);
        roommateBoardOptionRepository.saveAll(boardOptions);
        roommateBoardFileRepository.saveAll(boardFiles);
        roommateBoardInterestRepository.saveAll(boardInterests);
        searchRepository.saveAll(searches);
        inquiryRepository.saveAll(inquiries);
        inquiryCommentRepository.saveAll(inquiryComments);
        chattingRequiredRepository.saveAll(chattingRequirements);
        chattingRoomRepository.saveAll(chattingRooms);
        chatRoomMemberRepository.saveAll(chatRoomMembers);
        chatRoomMessageRepository.saveAll(chatMessages);
        chatRoomFileRepository.saveAll(chatRoomFiles);
        paymentRepository.saveAll(payments);
        pointRepository.saveAll(points);
        pointLogRepository.saveAll(pointLogs);

        seedAdditionalLoadTestData(
                recordCount,
                members,
                basicInformation,
                boards,
                chattingRequirements,
                chattingRooms,
                supportMember,
                adminMember,
                regions,
                roomTypes,
                profileFile
        );
        seedLoadTestLookupData(recordCount);
    }

    private void seedAdditionalLoadTestData(
            int recordCount,
            List<Member> members,
            List<BasicInformation> basicInformation,
            List<RoommateBoard> boards,
            List<ChattingRequired> chattingRequirements,
            List<ChattingRoom> chattingRooms,
            Member supportMember,
            Member adminMember,
            List<Region> regions,
            List<RoomType> roomTypes,
            File profileFile
    ) {
        LifePatternInformation patternInformation = lifePatternInformationRepository.findAll().get(0);
        LifePattern lifePattern = lifePatternRepository.findAll().get(0);

        List<MemberInterest> interests = new ArrayList<>(recordCount);
        List<Block> blocks = new ArrayList<>(recordCount);
        List<MemberDeclaration> memberDeclarations = new ArrayList<>(recordCount);
        List<RoommateBoardDeclaration> boardDeclarations = new ArrayList<>(recordCount);
        List<Faq> faqs = new ArrayList<>(recordCount);
        List<Authentication> authentications = new ArrayList<>(recordCount);
        List<AuthenticationApprove> authenticationApproves = new ArrayList<>(recordCount);
        List<BasicInformationFile> basicInformationFiles = new ArrayList<>(recordCount);
        List<RoomOfferProfile> offerProfiles = new ArrayList<>(recordCount);
        List<RoomSeekerProfile> seekerProfiles = new ArrayList<>(recordCount);
        List<OfferRoomType> offerRoomTypes = new ArrayList<>(recordCount);
        List<SeekerRoomType> seekerRoomTypes = new ArrayList<>(recordCount);
        List<RoomSeekerProfileRegion> seekerProfileRegions = new ArrayList<>(recordCount);
        List<MemberLifePattern> memberLifePatterns = new ArrayList<>(recordCount);
        List<PreferenceCondition> preferenceConditions = new ArrayList<>(recordCount);
        List<PreferenceConditionWeight> preferenceWeights = new ArrayList<>(recordCount);
        List<MemberLifePatternLogDegree> memberLogDegrees = new ArrayList<>(recordCount);
        List<PreferenceConditionLogDegree> preferenceLogDegrees = new ArrayList<>(recordCount);
        List<PreferenceConditionWeightLogDegree> weightLogDegrees = new ArrayList<>(recordCount);
        List<MemberLifePatternLog> memberLifePatternLogs = new ArrayList<>(recordCount);
        List<PreferenceConditionLog> preferenceConditionLogs = new ArrayList<>(recordCount);
        List<PreferenceConditionWeightLog> preferenceWeightLogs = new ArrayList<>(recordCount);
        List<ChattingScore> chattingScores = new ArrayList<>(recordCount);
        List<RoommateMatchingRequired> matchingRequirements = new ArrayList<>(recordCount);
        List<MyRoommate> roommates = new ArrayList<>(recordCount);
        List<RoommateScore> roommateScores = new ArrayList<>(recordCount);
        List<RoommateHouseRule> houseRules = new ArrayList<>(recordCount);
        List<RoommateCalendarCategory> calendarCategories = new ArrayList<>(recordCount);
        List<RoommateCalendar> calendars = new ArrayList<>(recordCount);
        List<RoommateCalendarMember> calendarMembers = new ArrayList<>(recordCount * 2);
        List<RepeatRoommateCalendar> repeatingCalendars = new ArrayList<>(recordCount);
        List<ExcludeRoommateCalendar> excludedCalendarOccurrences = new ArrayList<>(recordCount);
        List<AlarmSetting> alarmSettings = new ArrayList<>(recordCount);
        List<Alarm> alarms = new ArrayList<>(recordCount);
        List<Notification> notifications = new ArrayList<>(recordCount);
        List<NotificationAlarm> notificationAlarms = new ArrayList<>(recordCount);
        List<ChattingRequiredAlarm> chattingAlarms = new ArrayList<>(recordCount);
        List<RoommateMatchingRequiredAlarm> matchingAlarms = new ArrayList<>(recordCount);
        List<RoommateCalendarAlarm> calendarAlarms = new ArrayList<>(recordCount);

        List<Member> extraSeekerMembers = new ArrayList<>(recordCount);
        List<State> extraSeekerStates = new ArrayList<>(recordCount);
        List<BasicInformation> extraSeekerInformation = new ArrayList<>(recordCount);
        List<MemberPrivacy> extraSeekerPrivacy = new ArrayList<>(recordCount);
        for (int i = 1; i <= recordCount; i++) {
            Member seekerMember = Member.builder()
                    .providerType(i % 2 == 0 ? LoginProviderType.KAKAO : LoginProviderType.APPLE)
                    .providerId("load_test_seeker_" + i)
                    .role(MemberRole.USER)
                    .isDelete(false)
                    .build();
            extraSeekerMembers.add(seekerMember);
            extraSeekerStates.add(State.builder().member(seekerMember).states(MemberState.ACTIVE).build());
            extraSeekerPrivacy.add(MemberPrivacy.builder().member(seekerMember).type(MemberPrivacyType.PUBLIC).build());
            extraSeekerInformation.add(BasicInformation.builder().member(seekerMember)
                    .name("방찾기" + i).birth(LocalDate.of(1990 + i % 20, i % 12 + 1, i % 28 + 1))
                    .gender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                    .email("loadseeker" + i + "@example.com").build());
        }
        memberRepository.saveAll(extraSeekerMembers);
        stateRepository.saveAll(extraSeekerStates);
        memberPrivacyRepository.saveAll(extraSeekerPrivacy);
        basicInformationRepository.saveAll(extraSeekerInformation);

        for (int i = 0; i < recordCount; i++) {
            Member member = members.get(i);
            Member seekerMember = extraSeekerMembers.get(i);
            RoommateBoard board = boards.get(i);
            ChattingRequired chattingRequired = chattingRequirements.get(i);
            ChattingRoom chattingRoom = chattingRooms.get(i);
            BasicInformation info = basicInformation.get(i);
            RoomType roomType = roomTypes.get(i % roomTypes.size());
            Region region = regions.get(i % regions.size());

            interests.add(MemberInterest.builder().sender(member).receiver(supportMember).isDeleted(false).build());
            blocks.add(Block.builder().blocker(member).blocked(supportMember).isDeleted(false).build());
            memberDeclarations.add(MemberDeclaration.builder()
                    .reporterId(member.getId()).reportedId(supportMember.getId()).reason("부하 테스트 신고 " + (i + 1))
                    .declarationType(DeclarationType.PENDING).build());
            boardDeclarations.add(RoommateBoardDeclaration.builder()
                    .memberId(supportMember.getId()).roommateBoardId(board.getId()).reason("부하 테스트 게시글 신고 " + (i + 1))
                    .declarationType(DeclarationType.PENDING).build());
            faqs.add(Faq.builder()
                    .title("부하 테스트 FAQ " + (i + 1))
                    .contents("nGrinder FAQ 조회 테스트 데이터입니다. " + (i + 1))
                    .member(adminMember).sort(i + 1).isDeleted(false).build());

            Authentication authentication = Authentication.builder()
                    .memberId(member.getId()).type(AuthenticationType.STUDENT)
                    .email("loadtest" + (i + 1) + "@univ.ac.kr")
                    .code(String.format("%06d", (i + 1) % 1_000_000)).isAccepted(true).build();
            authentications.add(authentication);
            authenticationApproves.add(AuthenticationApprove.builder()
                    .authentication(authentication).status(ApproveType.ACCEPTED).build());
            basicInformationFiles.add(BasicInformationFile.builder().basicInformation(info).file(profileFile).build());

            RoomOfferProfile offerProfile = RoomOfferProfile.builder()
                    .member(member).region(region).deposit(300 + i % 20 * 50)
                    .monthlyRent(30 + i % 10 * 5).comeableAt(LocalDateTime.now().plusDays(i % 90 + 1))
                    .isComeableAtNegotiable(i % 2 == 0).build();
            offerProfiles.add(offerProfile);
            RoomSeekerProfile seekerProfile = RoomSeekerProfile.builder()
                    .member(seekerMember).minDeposit(100).maxDeposit(1000).minMonthlyRent(30).maxMonthlyRent(80)
                    .comeableAt(LocalDateTime.now().plusDays(i % 90 + 1))
                    .isComeableAtNegotiable(i % 2 == 0).build();
            seekerProfiles.add(seekerProfile);
            offerRoomTypes.add(OfferRoomType.builder().roomOfferProfile(offerProfile).roomType(roomType).build());
            seekerRoomTypes.add(SeekerRoomType.builder().roomSeekerProfile(seekerProfile).roomType(roomType).build());
            seekerProfileRegions.add(RoomSeekerProfileRegion.builder().roomSeekerProfile(seekerProfile).region(region).build());

            memberLifePatterns.add(MemberLifePattern.builder().member(member).lifePatternInformation(patternInformation).build());
            preferenceConditions.add(PreferenceCondition.builder().member(member).lifePatternInformation(patternInformation).build());
            preferenceWeights.add(PreferenceConditionWeight.builder().member(member).lifePattern(lifePattern).build());
            MemberLifePatternLogDegree memberDegree = MemberLifePatternLogDegree.builder().degree((long) i + 1).build();
            PreferenceConditionLogDegree preferenceDegree = PreferenceConditionLogDegree.builder().degree((long) i + 1).build();
            PreferenceConditionWeightLogDegree weightDegree = PreferenceConditionWeightLogDegree.builder().degree((long) i + 1).build();
            memberLogDegrees.add(memberDegree);
            preferenceLogDegrees.add(preferenceDegree);
            weightLogDegrees.add(weightDegree);
            memberLifePatternLogs.add(MemberLifePatternLog.builder().member(member)
                    .lifePatternInformation(patternInformation).memberLifePatternLogDegree(memberDegree).build());
            preferenceConditionLogs.add(PreferenceConditionLog.builder().member(member)
                    .lifePatternInformation(patternInformation).preferenceConditionLogDegree(preferenceDegree).build());
            preferenceWeightLogs.add(PreferenceConditionWeightLog.builder().member(member)
                    .lifePattern(lifePattern).preferenceConditionWeightLogDegree(weightDegree).build());
            ChattingScore chattingScore = ChattingScore.builder().chattingRequired(chattingRequired)
                    .memberLifePatternLogDegree(memberDegree).preferenceConditionLogDegree(preferenceDegree)
                    .preferenceConditionWeightLogDegree(weightDegree).score(50 + i % 51).build();
            chattingScores.add(chattingScore);

            RoommateMatchingRequired matching = RoommateMatchingRequired.builder()
                    .requester(member).requestee(supportMember).chattingRoom(chattingRoom)
                    .status(RoommateRequiredStatus.ACCEPTED).build();
            matchingRequirements.add(matching);
            MyRoommate roommate = MyRoommate.builder().roommateMatchingRequired(matching).isDeleted(false).build();
            roommates.add(roommate);
            roommateScores.add(RoommateScore.builder().myRoommate(roommate).chattingScore(chattingScore).build());
            houseRules.add(RoommateHouseRule.builder().member(member).myRoommate(roommate)
                    .title("부하 테스트 생활 규칙 " + (i + 1)).contents("공용 공간을 깨끗하게 사용합니다.")
                    .isDeleted(false).build());

            RoommateCalendarCategory category = RoommateCalendarCategory.builder().name("부하 테스트 일정 " + (i + 1)).build();
            calendarCategories.add(category);
            RoommateCalendar calendar = RoommateCalendar.builder().myRoommate(roommate).member(member)
                    .roommateCalendarCategory(category).title("부하 테스트 일정 " + (i + 1))
                    .contents("nGrinder 일정 조회 테스트 데이터입니다.")
                    .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusHours(1))
                    .isDeleted(false).build();
            calendars.add(calendar);
            calendarMembers.add(RoommateCalendarMember.of(calendar, member));
            calendarMembers.add(RoommateCalendarMember.of(calendar, supportMember));
            RepeatRoommateCalendar repeat = RepeatRoommateCalendar.builder().roommateCalendar(calendar)
                    .repeatType(RepeatType.WEEKLY).endDate(LocalDateTime.now().plusMonths(3)).build();
            repeatingCalendars.add(repeat);
            excludedCalendarOccurrences.add(ExcludeRoommateCalendar.builder().repeatRoommateCalendar(repeat)
                    .excludeAt(LocalDateTime.now().plusDays(7)).build());

            alarmSettings.add(AlarmSetting.builder().member(member)
                    .alarmSettingType(AlarmSettingType.NOTIFICATION).isEnabled(true).build());
            Alarm alarm = Alarm.builder().member(member).title("부하 테스트 알림 " + (i + 1))
                    .contents("nGrinder 알림 조회 테스트 데이터입니다.")
                    .expiredAt(LocalDateTime.now().plusDays(30)).isRead(false).build();
            alarms.add(alarm);
            Notification notification = Notification.builder().member(adminMember)
                    .title("부하 테스트 공지 " + (i + 1)).contents("nGrinder 공지 조회 테스트 데이터입니다.")
                    .isDeleted(false).build();
            notifications.add(notification);
            notificationAlarms.add(NotificationAlarm.builder().alarm(alarm).notification(notification).build());
            chattingAlarms.add(ChattingRequiredAlarm.builder().member(member).title("부하 테스트 채팅 알림")
                    .expiredAt(LocalDateTime.now().plusDays(30)).chattingRequired(chattingRequired).build());
            matchingAlarms.add(RoommateMatchingRequiredAlarm.builder().member(member).title("부하 테스트 매칭 알림")
                    .expiredAt(LocalDateTime.now().plusDays(30)).roommateMatchingRequired(matching).build());
            calendarAlarms.add(RoommateCalendarAlarm.builder().member(member).title("부하 테스트 일정 알림")
                    .expiredAt(LocalDateTime.now().plusDays(30)).roommateCalendar(calendar).build());
        }

        memberInterestRepository.saveAll(interests);
        blockRepository.saveAll(blocks);
        memberDeclarationRepository.saveAll(memberDeclarations);
        roommateBoardDeclarationRepository.saveAll(boardDeclarations);
        faqRepository.saveAll(faqs);
        authenticationRepository.saveAll(authentications);
        authenticationApproveRepository.saveAll(authenticationApproves);
        basicInformationFileRepository.saveAll(basicInformationFiles);
        roomOfferProfileRepository.saveAll(offerProfiles);
        roomSeekerProfileRepository.saveAll(seekerProfiles);
        offerRoomTypeRepository.saveAll(offerRoomTypes);
        seekerRoomTypeRepository.saveAll(seekerRoomTypes);
        roomSeekerProfileRegionRepository.saveAll(seekerProfileRegions);
        memberLifePatternRepository.saveAll(memberLifePatterns);
        preferenceConditionRepository.saveAll(preferenceConditions);
        preferenceConditionWeightRepository.saveAll(preferenceWeights);
        memberLifePatternLogDegreeRepository.saveAll(memberLogDegrees);
        preferenceConditionLogDegreeRepository.saveAll(preferenceLogDegrees);
        preferenceConditionWeightLogDegreeRepository.saveAll(weightLogDegrees);
        memberLifePatternLogRepository.saveAll(memberLifePatternLogs);
        preferenceConditionLogRepository.saveAll(preferenceConditionLogs);
        preferenceConditionWeightLogRepository.saveAll(preferenceWeightLogs);
        chattingScoreRepository.saveAll(chattingScores);
        roommateMatchingRequiredRepository.saveAll(matchingRequirements);
        myRoommateRepository.saveAll(roommates);
        roommateScoreRepository.saveAll(roommateScores);
        roommateHouseRuleRepository.saveAll(houseRules);
        roommateCalendarCategoryRepository.saveAll(calendarCategories);
        roommateCalendarRepository.saveAll(calendars);
        roommateCalendarMemberRepository.saveAll(calendarMembers);
        repeatRoommateCalendarRepository.saveAll(repeatingCalendars);
        excludeRoommateCalendarRepository.saveAll(excludedCalendarOccurrences);
        alarmSettingRepository.saveAll(alarmSettings);
        alarmRepository.saveAll(alarms);
        notificationRepository.saveAll(notifications);
        notificationAlarmRepository.saveAll(notificationAlarms);
        chattingRequiredAlarmRepository.saveAll(chattingAlarms);
        roommateMatchingRequiredAlarmRepository.saveAll(matchingAlarms);
        roommateCalendarAlarmRepository.saveAll(calendarAlarms);
    }

    private void seedLoadTestLookupData(int recordCount) {
        Region parentRegion = regionRepository.findAll().stream()
                .filter(region -> region.getScope() == 2)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Load test seed requires a level-2 region"));

        List<Region> regions = new ArrayList<>(recordCount);
        List<RoomType> roomTypes = new ArrayList<>(recordCount);
        List<RoomExtraOption> roomExtraOptions = new ArrayList<>(recordCount);
        List<LifePattern> lifePatterns = new ArrayList<>(recordCount);
        List<LifePatternInformation> patternInformations = new ArrayList<>(recordCount);
        List<InquiryCategory> inquiryCategories = new ArrayList<>(recordCount);
        List<AppVersion> appVersions = new ArrayList<>(recordCount);
        List<AuthEmail> authEmails = new ArrayList<>(recordCount);
        List<File> files = new ArrayList<>(recordCount);

        for (int i = 1; i <= recordCount; i++) {
            regions.add(Region.builder().name("부하테스트 지역 " + i).scope(3).parent(parentRegion).build());
            roomTypes.add(RoomType.builder().name("부하테스트 방 유형 " + i).isDeleted(false).build());
            roomExtraOptions.add(RoomExtraOption.builder().name("부하테스트 옵션 " + i).isDeleted(false).build());
            LifePattern lifePattern = LifePattern.builder().name("부하테스트 생활패턴 " + i)
                    .dtype(LifePatternType.SCALE)
                    .lifePatternDescription("nGrinder 생활패턴 조회 테스트 데이터")
                    .preferenceDescription("nGrinder 선호조건 조회 테스트 데이터")
                    .isDeleted(false).sort(i + 8).build();
            lifePatterns.add(lifePattern);
            patternInformations.add(LifePatternInformation.builder().lifePattern(lifePattern)
                    .dvalue("1").description("부하 테스트 선택지 " + i).build());
            inquiryCategories.add(InquiryCategory.builder().title("부하 테스트 문의 유형 " + i).isDeleted(false).build());
            appVersions.add(AppVersion.builder().platformType(i % 2 == 0 ? PlatformType.ANDROID : PlatformType.IOS)
                    .version("1.0." + i).minVersion("1.0.0").updateType(UpdateType.SELECT).isDeleted(false).build());
            authEmails.add(AuthEmail.builder().domain("loadtest" + i + ".example.com")
                    .name("부하 테스트 인증 도메인 " + i)
                    .dtype(i % 2 == 0 ? AuthenticationType.STUDENT : AuthenticationType.COMPANY)
                    .isDeleted(false).build());
            files.add(File.builder().type(FileType.ETC)
                    .originalFileName("load-test-" + i + ".jpg")
                    .savedFileName("load-test-" + i + ".jpg")
                    .fileExt("jpg").isDeleted(false).build());
        }

        regionRepository.saveAll(regions);
        roomTypeRepository.saveAll(roomTypes);
        roomExtraOptionRepository.saveAll(roomExtraOptions);
        lifePatternRepository.saveAll(lifePatterns);
        lifePatternInformationRepository.saveAll(patternInformations);
        inquiryCategoryRepository.saveAll(inquiryCategories);
        appVersionRepository.saveAll(appVersions);
        authEmailRepository.saveAll(authEmails);
        fileRepository.saveAll(files);

        List<RoomTypeFile> roomTypeFiles = new ArrayList<>(recordCount);
        List<RoomExtraOptionFile> roomExtraOptionFiles = new ArrayList<>(recordCount);
        List<LifePatternFile> lifePatternFiles = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            File file = files.get(i);
            roomTypeFiles.add(RoomTypeFile.builder().roomType(roomTypes.get(i)).file(file).build());
            roomExtraOptionFiles.add(RoomExtraOptionFile.builder().roomExtraOption(roomExtraOptions.get(i)).file(file).build());
            lifePatternFiles.add(LifePatternFile.builder().lifePattern(lifePatterns.get(i)).file(file).build());
        }
        roomTypeFileRepository.saveAll(roomTypeFiles);
        roomExtraOptionFileRepository.saveAll(roomExtraOptionFiles);
        lifePatternFileRepository.saveAll(lifePatternFiles);
    }

    private List<LifePatternInformation> createLifePatternInformation(
            LifePattern lifePattern,
            String... descriptions
    ) {
        List<LifePatternInformation> informationList = new ArrayList<>();
        for (int i = 0; i < descriptions.length; i++) {
            informationList.add(LifePatternInformation.builder()
                    .lifePattern(lifePattern)
                    .dvalue(String.valueOf(i + 1))
                    .description(descriptions[i])
                    .build());
        }
        return informationList;
    }
}
