package org.example.knockin.global.batch;

import org.example.knockin.support.IsolatedExternalServices;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.declaration.entity.MemberDeclaration;
import org.example.knockin.declaration.entity.RoommateBoardDeclaration;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryCategory;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.MemberLifePattern;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.entity.PreferenceCondition;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionLogDegree;
import org.example.knockin.life.entity.PreferenceConditionWeight;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.entity.PreferenceConditionWeightLogDegree;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationApprove;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@IsolatedExternalServices
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:member-delete-batch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "batch.member-delete.chunk-size=2", "batch.member-delete.cron=-",
        "spring.batch.job.enabled=false", "seed.load-test.records-per-entity=30",
        "logging.level.org.hibernate.SQL=WARN"
})
@ActiveProfiles("test")
class MemberDeleteBatchIntegrationTest {
    private static final List<String> EXTERNAL_REFERENCE_TABLES = List.of(
            "member_declaration", "roommate_board_declaration", "inquiry", "inquiry_comment",
            "authentication", "authentication_approve", "member_agreement", "member_life_pattern",
            "member_life_pattern_log", "preference_condition", "preference_condition_log",
            "preference_condition_weight", "preference_condition_weight_log");

    @Autowired JobOperator jobOperator;
    @Autowired Job memberDeleteJob;
    @Autowired JdbcTemplate jdbc;
    @Autowired MemberRepository members;
    @Autowired MemberServiceImpl memberService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void resetEligibility() {
        jdbc.update("UPDATE member SET is_delete = false, deleted_at = NULL");
    }

    @Test
    void deletesSeededMembersAcrossChunks() throws Exception {
        long members = jdbc.queryForObject("SELECT COUNT(*) FROM member", Long.class);
        assertThat(members).isGreaterThan(2);
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 14, 3, 0);
        // Delete one seeded member first, preserving other members and their shared relationships.
        Long oneMember = jdbc.queryForObject("SELECT id FROM member WHERE provider_id = 'user2_provider_789'", Long.class);
        jdbc.update("UPDATE member SET is_delete = true, deleted_at = ? WHERE id = ?", cutoff.minusDays(1), oneMember);
        long alarmsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM alarm", Long.class);
        Map<String, Long> referencesBefore = externalReferenceCounts();
        jdbc.execute("CREATE TABLE seeded_delete_blocker (member_id BIGINT REFERENCES member(id))");
        jdbc.update("INSERT INTO seeded_delete_blocker(member_id) VALUES (?)", oneMember);
        var partial = jobOperator.start(memberDeleteJob, new JobParametersBuilder()
                .addString("cutoff", cutoff.minusMinutes(1).toString()).addLong("chunkSize", 2L).toJobParameters());
        try {
            assertThat(partial.getStatus()).isEqualTo(BatchStatus.FAILED);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alarm", Long.class)).isEqualTo(alarmsBefore);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM member", Long.class)).isEqualTo(members);
            assertThat(externalReferenceCounts()).isEqualTo(referencesBefore);
        } finally {
            jdbc.execute("DROP TABLE seeded_delete_blocker");
        }
        partial = jobOperator.restart(partial);
        assertThat(partial.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM member", Long.class)).isEqualTo(members - 1);
        assertNoOrphanedExternalReferences();
        for (String[] subtype : new String[][]{{"CHATTING_REQUIRED", "chatting_required_alarm"},
                {"ROOM_MATCHING", "roommate_matching_required_alarm"}, {"ROOMMATE_CALENDAR", "roommate_calendar_alarm"}}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alarm a WHERE a.type = ? AND NOT EXISTS "
                    + "(SELECT 1 FROM " + subtype[1] + " child WHERE child.id = a.id)", Long.class, subtype[0]))
                    .as("JOINED alarm parent must not remain without %s", subtype[1]).isZero();
        }
        jdbc.update("UPDATE member SET is_delete = true, deleted_at = ?", cutoff.minusDays(1));

        var execution = jobOperator.start(memberDeleteJob,
                new JobParametersBuilder().addString("cutoff", cutoff.toString()).addLong("chunkSize", 2L).toJobParameters());

        assertThat(execution.getStatus()).withFailMessage("%s", execution.getAllFailureExceptions())
                .isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM member", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roommate_board", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM basic_information", Long.class)).isZero();
        externalReferenceCounts().forEach((table, count) ->
                assertThat(count).as("%s must be empty after deleting all members", table).isZero());
        var step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isEqualTo(members - 1);
        assertThat(step.getCommitCount()).isGreaterThan(1);
    }
    @Test
    void failedChunkRollsBackAndRestartsAfterLastCommittedId() throws Exception {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 14, 3, 1);
        List<Long> ids = IntStream.range(0, 5)
                .mapToObj(i -> member(true, cutoff.minusDays(1))).toList();
        Long survivor = member(false, null);
        List<List<RelatedRow>> references = ids.stream()
                .map(id -> externalReferences(id, survivor)).toList();
        jdbc.execute("CREATE TABLE batch_delete_blocker (member_id BIGINT REFERENCES member(id))");
        jdbc.update("INSERT INTO batch_delete_blocker(member_id) VALUES (?)", ids.get(3));
        try {
            var parameters = new JobParametersBuilder().addString("cutoff", cutoff.toString())
                    .addLong("chunkSize", 2L).toJobParameters();
            var failed = jobOperator.start(memberDeleteJob, parameters);
            assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
            assertThat(members.findAllById(ids)).extracting(Member::getId).containsExactlyInAnyOrderElementsOf(ids.subList(2, 5));
            assertThat(failed.getStepExecutions().iterator().next().getRollbackCount()).isPositive();
            for (int i = 0; i < references.size(); i++) {
                assertRelatedRows(references.get(i), i >= 2);
            }

            jdbc.update("DELETE FROM batch_delete_blocker");
            var restarted = jobOperator.restart(failed);
            assertThat(restarted.getStatus()).withFailMessage("%s", restarted.getAllFailureExceptions())
                    .isEqualTo(BatchStatus.COMPLETED);
            assertThat(restarted.getJobInstance().getInstanceId()).isEqualTo(failed.getJobInstance().getInstanceId());
            assertThat(restarted.getStepExecutions().iterator().next().getReadCount()).isEqualTo(3);
            assertThat(members.findAllById(ids)).isEmpty();
            references.forEach(rows -> assertRelatedRows(rows, false));
            assertThat(members.findById(survivor)).isPresent();
            assertNoOrphanedExternalReferences();
        } finally {
            jdbc.execute("DROP TABLE batch_delete_blocker");
        }
    }

    @Test
    void excludesActiveRecentBoundaryAndMissingDeletionDates() throws Exception {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 14, 3, 2);
        Long old = member(true, cutoff.minusDays(1));
        List<Long> protectedIds = List.of(member(false, cutoff.minusDays(1)),
                member(true, cutoff.plusDays(1)), member(true, cutoff), member(true, null));
        var execution = jobOperator.start(memberDeleteJob, new JobParametersBuilder()
                .addString("cutoff", cutoff.toString()).addLong("chunkSize", 2L).toJobParameters());
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(members.findById(old)).isEmpty();
        assertThat(members.findAllById(protectedIds)).hasSize(4);
    }

    @Test
    void writerRechecksEligibilityEvenWhenReaderAlreadySelectedMember() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 14, 3, 3);
        Long recovered = member(true, cutoff.minusDays(1));
        Long deletedAgain = member(true, cutoff.minusDays(1));
        Long eligible = member(true, cutoff.minusDays(1));
        Long survivor = member(false, null);
        List<RelatedRow> recoveredReferences = externalReferences(recovered, survivor);
        List<RelatedRow> deletedAgainReferences = externalReferences(deletedAgain, survivor);
        List<RelatedRow> eligibleReferences = externalReferences(eligible, survivor);
        // These IDs represent the reader's snapshot before recovery/new withdrawal.
        List<Long> selectedIds = List.of(recovered, deletedAgain, eligible);
        jdbc.update("UPDATE member SET is_delete = false, deleted_at = NULL WHERE id = ?", recovered);
        jdbc.update("UPDATE member SET deleted_at = ? WHERE id = ?", cutoff.plusDays(1), deletedAgain);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                memberService.hardDeleteMember(selectedIds, cutoff));
        assertThat(members.findAllById(selectedIds)).extracting(Member::getId)
                .containsExactlyInAnyOrder(recovered, deletedAgain);
        assertRelatedRows(recoveredReferences, true);
        assertRelatedRows(deletedAgainReferences, true);
        assertRelatedRows(eligibleReferences, false);
        assertNoOrphanedExternalReferences();
    }

    private Map<String, Long> externalReferenceCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        EXTERNAL_REFERENCE_TABLES.forEach(table ->
                counts.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)));
        return counts;
    }

    private void assertNoOrphanedExternalReferences() {
        for (String[] reference : new String[][]{
                {"member_declaration", "reporter_id", "member"},
                {"member_declaration", "reported_id", "member"},
                {"roommate_board_declaration", "member_id", "member"},
                {"roommate_board_declaration", "roommate_board_id", "roommate_board"},
                {"inquiry", "member_id", "member"}, {"inquiry_comment", "member_id", "member"},
                {"inquiry_comment", "inquiry_id", "inquiry"},
                {"authentication", "member_id", "member"},
                {"authentication_approve", "authentication_id", "authentication"},
                {"member_agreement", "member_id", "member"},
                {"member_life_pattern", "member_id", "member"},
                {"member_life_pattern_log", "member_id", "member"},
                {"preference_condition", "member_id", "member"},
                {"preference_condition_log", "member_id", "member"},
                {"preference_condition_weight", "member_id", "member"},
                {"preference_condition_weight_log", "member_id", "member"}}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + reference[0] + " child WHERE child."
                    + reference[1] + " IS NOT NULL AND NOT EXISTS (SELECT 1 FROM " + reference[2]
                    + " parent WHERE parent.id = child." + reference[1] + ")", Long.class))
                    .as("%s.%s must reference an existing %s", (Object[]) reference).isZero();
        }
    }

    private List<RelatedRow> externalReferences(Long memberId, Long survivorId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            LifePatternInformation information = first(LifePatternInformation.class);
            AgreementLog agreementLog = first(AgreementLog.class);
            InquiryCategory category = first(InquiryCategory.class);
            RoomType roomType = first(RoomType.class);
            Region region = first(Region.class);

            RoommateBoard ownedBoard = RoommateBoard.builder().member(entityManager.find(Member.class, memberId))
                    .roomType(roomType).region(region).build();
            RoommateBoard survivingBoard = RoommateBoard.builder().member(entityManager.find(Member.class, survivorId))
                    .roomType(roomType).region(region).build();
            entityManager.persist(ownedBoard);
            entityManager.persist(survivingBoard);

            MemberDeclaration submittedReport = MemberDeclaration.builder().reporterId(memberId).reportedId(survivorId).build();
            MemberDeclaration receivedReport = MemberDeclaration.builder().reporterId(survivorId).reportedId(memberId).build();
            RoommateBoardDeclaration submittedBoardReport = RoommateBoardDeclaration.builder()
                    .memberId(memberId).roommateBoardId(survivingBoard.getId()).build();
            RoommateBoardDeclaration receivedBoardReport = RoommateBoardDeclaration.builder()
                    .memberId(survivorId).roommateBoardId(ownedBoard.getId()).build();

            Inquiry ownedInquiry = Inquiry.builder().memberId(memberId).inquiryCategory(category).contents("owned inquiry").build();
            Inquiry survivingInquiry = Inquiry.builder().memberId(survivorId).inquiryCategory(category).contents("surviving inquiry").build();
            entityManager.persist(ownedInquiry);
            entityManager.persist(survivingInquiry);
            InquiryComment receivedComment = InquiryComment.builder().memberId(survivorId).inquiry(ownedInquiry).contents("reply").build();
            InquiryComment submittedComment = InquiryComment.builder().memberId(memberId).inquiry(survivingInquiry).contents("reply").build();

            Authentication authentication = Authentication.builder().memberId(memberId).type(AuthenticationType.STUDENT)
                    .email("batch@example.com").code("123456").build();
            entityManager.persist(authentication);
            AuthenticationApprove approve = AuthenticationApprove.builder().authentication(authentication).status(ApproveType.ACCEPTED).build();
            MemberAgreement agreement = MemberAgreement.builder().memberId(memberId).agreementLog(agreementLog).isAgreed(true).build();

            MemberLifePatternLogDegree lifestyleDegree = MemberLifePatternLogDegree.builder().degree(1L).build();
            PreferenceConditionLogDegree conditionDegree = PreferenceConditionLogDegree.builder().degree(1L).build();
            PreferenceConditionWeightLogDegree weightDegree = PreferenceConditionWeightLogDegree.builder().degree(1L).build();
            entityManager.persist(lifestyleDegree);
            entityManager.persist(conditionDegree);
            entityManager.persist(weightDegree);
            MemberLifePattern lifestyle = MemberLifePattern.builder().memberId(memberId).lifePatternInformation(information).build();
            MemberLifePatternLog lifestyleLog = MemberLifePatternLog.builder().memberId(memberId).lifePatternInformation(information)
                    .memberLifePatternLogDegree(lifestyleDegree).build();
            PreferenceCondition condition = PreferenceCondition.builder().memberId(memberId).lifePatternInformation(information).build();
            PreferenceConditionLog conditionLog = PreferenceConditionLog.builder().memberId(memberId).lifePatternInformation(information)
                    .preferenceConditionLogDegree(conditionDegree).build();
            PreferenceConditionWeight weight = PreferenceConditionWeight.builder().memberId(memberId).lifePattern(information.getLifePattern()).build();
            PreferenceConditionWeightLog weightLog = PreferenceConditionWeightLog.builder().memberId(memberId).lifePattern(information.getLifePattern())
                    .preferenceConditionWeightLogDegree(weightDegree).build();
            List.of(submittedReport, receivedReport, submittedBoardReport, receivedBoardReport, receivedComment, submittedComment,
                    approve, agreement, lifestyle, lifestyleLog, condition, conditionLog, weight, weightLog).forEach(entityManager::persist);
            entityManager.flush();
            return List.of(
                    new RelatedRow("member_declaration", "id", submittedReport.getId()),
                    new RelatedRow("member_declaration", "id", receivedReport.getId()),
                    new RelatedRow("roommate_board_declaration", "id", submittedBoardReport.getId()),
                    new RelatedRow("roommate_board_declaration", "id", receivedBoardReport.getId()),
                    new RelatedRow("inquiry", "id", ownedInquiry.getId()),
                    new RelatedRow("inquiry_comment", "id", receivedComment.getId()),
                    new RelatedRow("inquiry_comment", "id", submittedComment.getId()),
                    new RelatedRow("authentication", "id", authentication.getId()),
                    new RelatedRow("authentication_approve", "authentication_id", approve.getId()),
                    new RelatedRow("member_agreement", "id", agreement.getId()),
                    new RelatedRow("member_life_pattern", "id", lifestyle.getId()),
                    new RelatedRow("member_life_pattern_log", "id", lifestyleLog.getId()),
                    new RelatedRow("preference_condition", "id", condition.getId()),
                    new RelatedRow("preference_condition_log", "id", conditionLog.getId()),
                    new RelatedRow("preference_condition_weight", "id", weight.getId()),
                    new RelatedRow("preference_condition_weight_log", "id", weightLog.getId()));
        });
    }

    private <T> T first(Class<T> type) {
        return entityManager.createQuery("SELECT item FROM " + type.getSimpleName() + " item", type)
                .setMaxResults(1).getSingleResult();
    }

    private void assertRelatedRows(List<RelatedRow> rows, boolean present) {
        rows.forEach(row -> assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + row.table()
                + " WHERE " + row.idColumn() + " = ?", Long.class, row.id()))
                .as("%s row %s must be %s", row.table(), row.id(), present ? "preserved" : "deleted")
                .isEqualTo(present ? 1L : 0L));
    }

    private record RelatedRow(String table, String idColumn, Long id) {}

    private Long member(boolean deleted, LocalDateTime deletedAt) {
        return members.save(Member.builder().providerType(LoginProviderType.KAKAO)
                .providerId(UUID.randomUUID().toString()).role(MemberRole.USER)
                .isDelete(deleted).deletedAt(deletedAt).build()).getId();
    }

}
