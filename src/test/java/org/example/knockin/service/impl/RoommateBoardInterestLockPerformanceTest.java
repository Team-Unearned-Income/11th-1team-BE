package org.example.knockin.service.impl;

import org.example.knockin.support.IsolatedExternalServices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.board.entity.RoommateBoard;
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
@Tag("performance")
@DisplayName("룸메이트 게시글 좋아요 락 성능 관찰")
class RoommateBoardInterestLockPerformanceTest {
    private static final int THREAD_COUNT = 30;

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
    @DisplayName("같은 게시글 경합과 서로 다른 게시글 요청의 처리 시간을 비교한다")
    void compareSameBoardAndDifferentBoardLockContention() throws Exception {
        SameBoardScenario sameBoardScenario = createSameBoardScenario(THREAD_COUNT);
        PerformanceResult sameBoardResult = runConcurrentToggle(
                "same-board-different-members",
                sameBoardScenario.requests());

        DifferentBoardScenario differentBoardScenario = createDifferentBoardScenario(THREAD_COUNT);
        PerformanceResult differentBoardResult = runConcurrentToggle(
                "different-boards-different-members",
                differentBoardScenario.requests());
        writeReport(List.of(sameBoardResult, differentBoardResult));

        assertThat(sameBoardResult.failureCount()).isZero();
        assertThat(differentBoardResult.failureCount()).isZero();
    }

    private void writeReport(List<PerformanceResult> results) throws IOException {
        Path reportPath = Path.of("build", "reports", "performance", "roommate-board-interest-lock.md");
        Files.createDirectories(reportPath.getParent());

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Roommate Board Interest Lock Performance\n\n");
        markdown.append("| scenario | threads | total ms | success | failure | avg latency ms | max latency ms |\n");
        markdown.append("|---|---:|---:|---:|---:|---:|---:|\n");

        for (PerformanceResult result : results) {
            markdown.append("| ")
                    .append(result.scenarioName())
                    .append(" | ")
                    .append(result.threadCount())
                    .append(" | ")
                    .append(result.totalElapsedMs())
                    .append(" | ")
                    .append(result.successCount())
                    .append(" | ")
                    .append(result.failureCount())
                    .append(" | ")
                    .append("%.2f".formatted(result.avgLatencyMs()))
                    .append(" | ")
                    .append(result.maxLatencyMs())
                    .append(" |\n");
        }

        Files.writeString(reportPath, markdown.toString());
        System.out.println("performanceReport=" + reportPath.toAbsolutePath());
    }

    private PerformanceResult runConcurrentToggle(String scenarioName, List<ToggleRequest> requests) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requests.size());
        CountDownLatch readyLatch = new CountDownLatch(requests.size());
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<ToggleResult>> tasks = requests.stream()
                .map(request -> (Callable<ToggleResult>) () -> {
                    readyLatch.countDown();
                    startLatch.await();

                    long startedAt = System.nanoTime();
                    try {
                        roommateBoardService.likeBoard(request.boardId(), request.memberId());
                        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                        return ToggleResult.success((int) elapsedMs);
                    } catch (Exception e) {
                        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                        return ToggleResult.failure((int) elapsedMs, e);
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

        long successCount = results.stream().filter(ToggleResult::success).count();
        long failureCount = results.size() - successCount;
        IntSummaryStatistics latencyStats = results.stream()
                .mapToInt(ToggleResult::elapsedMs)
                .summaryStatistics();

        PerformanceResult result = new PerformanceResult(
                scenarioName,
                requests.size(),
                totalElapsedMs,
                successCount,
                failureCount,
                latencyStats.getAverage(),
                latencyStats.getMax());
        System.out.println(result.toLogLine());

        results.stream()
                .filter(resultItem -> !resultItem.success())
                .findFirst()
                .ifPresent(resultItem -> System.out.println("firstFailure=" + resultItem.exception()));

        return result;
    }

    private SameBoardScenario createSameBoardScenario(int requestCount) {
        return transactionTemplate.execute(status -> {
            String suffix = shortSuffix();
            Member boardOwner = memberRepository.save(createMember("owner-same-" + suffix));
            RoomType roomType = roomTypeRepository.save(createRoomType("원룸"));
            Region region = regionRepository.save(createRegion("역삼동", 3, null));
            RoommateBoard board = roommateBoardRepository.save(createBoard(boardOwner, roomType, region, "같은 게시글"));

            List<ToggleRequest> requests = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                Member liker = memberRepository.save(createMember("same-liker-" + suffix + "-" + i));
                requests.add(new ToggleRequest(board.getId(), liker.getId()));
            }
            return new SameBoardScenario(requests);
        });
    }

    private DifferentBoardScenario createDifferentBoardScenario(int requestCount) {
        return transactionTemplate.execute(status -> {
            String suffix = shortSuffix();
            Member boardOwner = memberRepository.save(createMember("owner-different-" + suffix));
            RoomType roomType = roomTypeRepository.save(createRoomType("투룸"));
            Region region = regionRepository.save(createRegion("서초동", 3, null));

            List<ToggleRequest> requests = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                RoommateBoard board = roommateBoardRepository.save(createBoard(
                        boardOwner,
                        roomType,
                        region,
                        "서로 다른 게시글 " + i));
                Member liker = memberRepository.save(createMember("different-liker-" + suffix + "-" + i));
                requests.add(new ToggleRequest(board.getId(), liker.getId()));
            }
            return new DifferentBoardScenario(requests);
        });
    }

    private Member createMember(String providerId) {
        return Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId(providerId)
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
    }

    private String shortSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private RoommateBoard createBoard(Member member, RoomType roomType, Region region, String title) {
        return RoommateBoard.builder()
                .memberId(member.getId())
                .title(title)
                .contents("락 성능 관찰용 게시글")
                .deposit(1_000)
                .monthlyRent(50)
                .managementCost(10)
                .roomTypeId(roomType.getId())
                .regionId(region.getId())
                .comeableDate(LocalDateTime.of(2026, 7, 1, 9, 0))
                .build();
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

    private record ToggleRequest(Long boardId, Long memberId) {
    }

    private record SameBoardScenario(List<ToggleRequest> requests) {
    }

    private record DifferentBoardScenario(List<ToggleRequest> requests) {
    }

    private record ToggleResult(boolean success, int elapsedMs, Exception exception) {
        private static ToggleResult success(int elapsedMs) {
            return new ToggleResult(true, elapsedMs, null);
        }

        private static ToggleResult failure(int elapsedMs, Exception exception) {
            return new ToggleResult(false, elapsedMs, exception);
        }
    }

    private record PerformanceResult(
            String scenarioName,
            int threadCount,
            long totalElapsedMs,
            long successCount,
            long failureCount,
            double avgLatencyMs,
            int maxLatencyMs
    ) {
        private String toLogLine() {
            return "scenario=%s, threadCount=%d, totalElapsedMs=%d, successCount=%d, failureCount=%d, avgLatencyMs=%.2f, maxLatencyMs=%d"
                    .formatted(
                            scenarioName,
                            threadCount,
                            totalElapsedMs,
                            successCount,
                            failureCount,
                            avgLatencyMs,
                            maxLatencyMs);
        }
    }
}
