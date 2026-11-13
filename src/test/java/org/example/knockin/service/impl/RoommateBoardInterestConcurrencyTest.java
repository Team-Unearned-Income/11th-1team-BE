package org.example.knockin.service.impl;

import org.example.knockin.support.IsolatedExternalServices;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.board.repository.RoommateBoardInterestRepository;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.meta.repository.RegionRepository;
import org.example.knockin.room.repository.RoomTypeRepository;
import org.example.knockin.board.service.RoommateBoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@IsolatedExternalServices
@SpringBootTest(properties = "seed.load-test.records-per-entity=0")
@ActiveProfiles("test")
@Tag("concurrency")
@DisplayName("룸메이트 게시글 좋아요 동시성")
class RoommateBoardInterestConcurrencyTest {
    @Autowired
    private RoommateBoardService roommateBoardService;

    @Autowired
    private RoommateBoardInterestRepository roommateBoardInterestRepository;

    @Autowired
    private RoommateBoardRepository roommateBoardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("같은 회원이 같은 게시글을 동시에 좋아요해도 관심 row는 하나만 생성된다")
    void likeBoardCreatesOnlyOneInterestWhenRequestedConcurrently() throws Exception {
        TestData testData = transactionTemplate.execute(status -> {
            Member member = memberRepository.save(Member.builder()
                    .providerType(LoginProviderType.KAKAO)
                    .providerId("concurrency-member")
                    .role(MemberRole.USER)
                    .isDelete(false)
                    .build());
            RoomType roomType = roomTypeRepository.save(createRoomType("원룸"));
            Region region = regionRepository.save(createRegion("역삼동", 3, null));
            RoommateBoard board = roommateBoardRepository.save(RoommateBoard.builder()
                    .memberId(member.getId())
                    .title("동시성 테스트 게시글")
                    .contents("동시에 좋아요를 누르는 상황")
                    .deposit(1_000)
                    .monthlyRent(50)
                    .managementCost(10)
                    .roomTypeId(roomType.getId())
                    .regionId(region.getId())
                    .comeableDate(LocalDateTime.of(2026, 7, 1, 9, 0))
                    .build());
            return new TestData(board.getId(), member.getId());
        });

        int threadCount = 21;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<ToggleResult>> tasks = java.util.stream.IntStream.range(0, threadCount)
                .mapToObj(i -> (Callable<ToggleResult>) () -> {
                    readyLatch.countDown();
                    startLatch.await();
                    long startedAt = System.nanoTime();
                    try {
                        roommateBoardService.likeBoard(testData.boardId(), testData.memberId());
                        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                        return ToggleResult.success(i, (int) elapsedMs);
                    } catch (Exception e) {
                        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                        return ToggleResult.failure(i, (int) elapsedMs, e);
                    }
                })
                .toList();

        List<Future<ToggleResult>> futures = tasks.stream()
                .map(executorService::submit)
                .toList();

        long totalStartedAt = System.nanoTime();
        readyLatch.await();
        startLatch.countDown();

        List<ToggleResult> results = new ArrayList<>();
        for (Future<ToggleResult> future : futures) {
            results.add(future.get());
        }
        long totalElapsedMs = (System.nanoTime() - totalStartedAt) / 1_000_000;
        executorService.shutdown();

        List<RoommateBoardInterest> interests = roommateBoardInterestRepository.findAllByRoommateBoardIdAndMemberId(
                testData.boardId(), testData.memberId());
        ConcurrencyReport report = new ConcurrencyReport(
                threadCount,
                totalElapsedMs,
                results.stream().filter(ToggleResult::success).count(),
                results.stream().filter(result -> !result.success()).count(),
                interests.size(),
                interests.isEmpty() ? null : interests.getFirst().getIsDeleted(),
                false,
                results);
        writeReport(report);

        assertThat(report.failureCount()).isZero();
        assertThat(interests).hasSize(1);
        assertThat(interests.getFirst().getIsDeleted()).isFalse();
    }

    private void writeReport(ConcurrencyReport report) throws IOException {
        Path reportPath = Path.of("build", "reports", "concurrency", "roommate-board-interest-toggle.md");
        Files.createDirectories(reportPath.getParent());

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Roommate Board Interest Toggle Concurrency\n\n");
        markdown.append("## Summary\n\n");
        markdown.append("| threads | total ms | success | failure | final row count | expected isDeleted | actual isDeleted |\n");
        markdown.append("|---:|---:|---:|---:|---:|---:|---:|\n");
        markdown.append("| ")
                .append(report.threadCount())
                .append(" | ")
                .append(report.totalElapsedMs())
                .append(" | ")
                .append(report.successCount())
                .append(" | ")
                .append(report.failureCount())
                .append(" | ")
                .append(report.finalRowCount())
                .append(" | ")
                .append(report.expectedDeleted())
                .append(" | ")
                .append(report.actualDeleted())
                .append(" |\n\n");

        markdown.append("## Thread Results\n\n");
        markdown.append("| request | success | elapsed ms | exception |\n");
        markdown.append("|---:|:---:|---:|---|\n");
        report.results().stream()
                .sorted(java.util.Comparator.comparingInt(ToggleResult::requestIndex))
                .forEach(result -> markdown.append("| ")
                        .append(result.requestIndex())
                        .append(" | ")
                        .append(result.success())
                        .append(" | ")
                        .append(result.elapsedMs())
                        .append(" | ")
                        .append(result.exceptionName())
                        .append(" |\n"));

        Files.writeString(reportPath, markdown.toString());
        System.out.println("concurrencyReport=" + reportPath.toAbsolutePath());
    }

    private RoomType createRoomType(String name) {
        RoomType roomType = newInstance(RoomType.class);
        ReflectionTestUtils.setField(roomType, "name", name);
        ReflectionTestUtils.setField(roomType, "isDeleted", false);
        return roomType;
    }

    private Region createRegion(String name, Integer scope, Region parent) {
        Region region = newInstance(Region.class);
        ReflectionTestUtils.setField(region, "name", name);
        ReflectionTestUtils.setField(region, "scope", scope);
        ReflectionTestUtils.setField(region, "parent", parent);
        return region;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", e);
        }
    }

    private record TestData(Long boardId, Long memberId) {
    }

    private record ToggleResult(int requestIndex, boolean success, int elapsedMs, Exception exception) {
        private static ToggleResult success(int requestIndex, int elapsedMs) {
            return new ToggleResult(requestIndex, true, elapsedMs, null);
        }

        private static ToggleResult failure(int requestIndex, int elapsedMs, Exception exception) {
            return new ToggleResult(requestIndex, false, elapsedMs, exception);
        }

        private String exceptionName() {
            return exception == null ? "" : exception.getClass().getSimpleName();
        }
    }

    private record ConcurrencyReport(
            int threadCount,
            long totalElapsedMs,
            long successCount,
            long failureCount,
            int finalRowCount,
            Boolean actualDeleted,
            boolean expectedDeleted,
            List<ToggleResult> results
    ) {
    }
}
