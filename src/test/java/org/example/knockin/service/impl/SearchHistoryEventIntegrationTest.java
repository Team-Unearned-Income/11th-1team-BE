package org.example.knockin.service.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.board.event.BoardKeywordSearchedEvent;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.board.service.RoommateBoardService;
import org.example.knockin.board.service.impl.RoommateBoardFileServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardInterestServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardOptionServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardServiceImpl;
import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.global.config.RoommateBoardPolicy;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.Search;
import org.example.knockin.meta.event.SearchHistoryEventListener;
import org.example.knockin.meta.repository.SearchRepository;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.meta.service.impl.SearchServiceImpl;
import org.example.knockin.util.service.RoommateScoreService;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(SearchHistoryEventIntegrationTest.TestConfig.class)
@DisplayName("검색 이력 이벤트의 트랜잭션 및 비동기 처리")
class SearchHistoryEventIntegrationTest {

    @Autowired private RoommateBoardService boardService;
    @Autowired private RoommateBoardRepository boardRepository;
    @Autowired private SearchRepository searchRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ControlledExecutor executor;

    private final List<WriteObservation> writes = new CopyOnWriteArrayList<>();
    private Long memberId;

    @BeforeEach
    void setUp() throws Exception {
        executor.awaitIdle();
        executor.resetSubmittedCount();
        reset(boardRepository, searchRepository, memberRepository);
        writes.clear();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createQuery("delete from Search").executeUpdate();
            entityManager.createQuery("delete from Member").executeUpdate();
            Member member = Member.builder()
                    .providerType(LoginProviderType.KAKAO)
                    .providerId("search-history-integration")
                    .role(MemberRole.USER)
                    .build();
            entityManager.persist(member);
            memberId = member.getId();
        });

        // The repository adapters keep the context small while writes use real JPA and H2.
        when(memberRepository.getReferenceById(anyLong())).thenAnswer(invocation ->
                entityManager.getReference(Member.class, invocation.getArgument(0, Long.class)));
        when(searchRepository.save(any(Search.class))).thenAnswer(invocation -> {
            writes.add(new WriteObservation(
                    Thread.currentThread(),
                    TransactionSynchronizationManager.isActualTransactionActive(),
                    TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                    EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory)));
            Search search = invocation.getArgument(0);
            entityManager.persist(search);
            return search;
        });
        when(boardRepository.search(any(BoardListDto.Request.class), any(), any(LocalDateTime.class), anyLong()))
                .thenAnswer(invocation -> new SliceImpl<>(List.of(), invocation.getArgument(1), false));
    }

    @AfterEach
    void finishAsyncWork() throws Exception {
        executor.awaitIdle();
    }

    @Test
    @DisplayName("목록 조회는 읽기 전용이며 커밋 후 다른 스레드의 새 쓰기 트랜잭션으로 이력을 저장한다")
    void savesOnlyAfterReadOnlyQueryCommitsOnIndependentAsyncTransaction() throws Exception {
        Thread queryThread = Thread.currentThread();
        AtomicReference<EntityManager> queryEntityManager = new AtomicReference<>();
        BoardListDto.Request request = request("  원룸  ");
        PageRequest pageable = PageRequest.of(0, 20);
        when(boardRepository.search(eq(request), eq(pageable), any(LocalDateTime.class), eq(memberId)))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
                    queryEntityManager.set(
                            EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory));
                    assertThat(executor.submittedCount()).isZero();
                    assertThat(searchCount()).isZero();
                    return new SliceImpl<>(List.of(), pageable, false);
                });
        executor.pause();

        var result = boardService.getBoardList(request, pageable, memberId);

        assertThat(result.isEmpty()).isTrue();
        executor.awaitTaskStarted();
        assertThat(writes).isEmpty();
        assertThat(searchCount()).isZero();

        executor.awaitIdle();

        assertThat(writes).hasSize(1);
        WriteObservation write = writes.getFirst();
        assertThat(write.thread()).isNotSameAs(queryThread);
        assertThat(write.transactionActive()).isTrue();
        assertThat(write.readOnly()).isFalse();
        assertThat(queryEntityManager.get()).isNotNull();
        assertThat(write.entityManager()).isNotNull().isNotSameAs(queryEntityManager.get());
        assertThat(savedKeywords()).containsExactly("원룸");
    }

    @Test
    @DisplayName("목록 조회가 롤백되면 검색 이력 이벤트는 실행하지 않는다")
    void doesNotSaveWhenQueryRollsBack() throws Exception {
        when(boardRepository.search(any(BoardListDto.Request.class), any(), any(LocalDateTime.class), eq(memberId)))
                .thenThrow(new IllegalStateException("board query failed"));

        assertThatThrownBy(() -> boardService.getBoardList(request("원룸"), PageRequest.of(0, 20), memberId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("board query failed");
        executor.awaitIdle();

        assertThat(executor.submittedCount()).isZero();
        assertThat(writes).isEmpty();
        assertThat(searchCount()).isZero();
    }

    @Test
    @DisplayName("트랜잭션 없이 발행한 검색 이력 이벤트는 실행하지 않는다")
    void doesNotSaveWhenPublishedOutsideTransaction() throws Exception {
        eventPublisher.publishEvent(new BoardKeywordSearchedEvent(memberId, "원룸"));
        executor.awaitIdle();

        assertThat(executor.submittedCount()).isZero();
        assertThat(writes).isEmpty();
        assertThat(searchCount()).isZero();
    }

    @Test
    @DisplayName("이력 저장의 외래 키 오류는 목록 응답에 영향을 주지 않고 후속 이벤트도 처리한다")
    void failedHistoryWriteDoesNotFailQueryOrSubsequentEvents() throws Exception {
        executor.pause();

        var result = boardService.getBoardList(request("삭제된 회원의 검색"), PageRequest.of(0, 20), Long.MAX_VALUE);

        assertThat(result.isEmpty()).isTrue();
        executor.awaitTaskStarted();
        assertThat(writes).isEmpty();
        executor.awaitIdle();
        assertThat(writes).hasSize(1);
        assertThat(searchCount()).isZero();

        boardService.getBoardList(request("정상 검색"), PageRequest.of(0, 20), memberId);
        executor.awaitIdle();

        assertThat(writes).hasSize(2);
        assertThat(savedKeywords()).containsExactly("정상 검색");
    }

    private BoardListDto.Request request(String keyword) {
        BoardListDto.Request request = new BoardListDto.Request();
        request.setKeyword(keyword);
        return request;
    }

    private long searchCount() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        return transaction.execute(status ->
                entityManager.createQuery("select count(s) from Search s", Long.class).getSingleResult());
    }

    private List<String> savedKeywords() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        return transaction.execute(status ->
                entityManager.createQuery("select s.keyword from Search s order by s.id", String.class).getResultList());
    }

    private record WriteObservation(Thread thread, boolean transactionActive, boolean readOnly, EntityManager entityManager) {
    }

    @Configuration
    @EnableAsync
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({SearchServiceImpl.class, SearchHistoryEventListener.class})
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:search-history-events;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setManagedTypes(PersistenceManagedTypes.of(Member.class.getName(), Search.class.getName()));
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        EntityManager entityManager(EntityManagerFactory factory) {
            return SharedEntityManagerCreator.createSharedEntityManager(factory);
        }

        @Bean
        SearchRepository searchRepository() {
            return mock(SearchRepository.class);
        }

        @Bean
        MemberRepository memberRepository() {
            return mock(MemberRepository.class);
        }

        @Bean
        RoommateBoardRepository boardRepository() {
            return mock(RoommateBoardRepository.class);
        }

        @Bean
        RoommateBoardServiceImpl boardService(RoommateBoardRepository repository, ApplicationEventPublisher publisher) {
            RoommateBoardPolicy policy = new RoommateBoardPolicy();
            policy.setComeableDateVisibleGraceDays(7);
            return new RoommateBoardServiceImpl(
                    repository,
                    mock(MemberServiceImpl.class),
                    mock(MetaServiceImpl.class),
                    mock(RoommateScoreService.class),
                    mock(RoommateBoardFileServiceImpl.class),
                    mock(PreferenceConditionServiceImpl.class),
                    mock(MemberLifePatternService.class),
                    mock(AuthenticationServiceImpl.class),
                    mock(RoommateBoardOptionServiceImpl.class),
                    mock(RoommateBoardInterestServiceImpl.class),
                    policy,
                    publisher,
                    mock(AlarmServiceImpl.class),
                    mock(PushNotificationServiceImpl.class),
                    mock(DeclarationServiceImpl.class));
        }

        @Bean(name = "taskExecutor", destroyMethod = "close")
        ControlledExecutor taskExecutor() {
            return new ControlledExecutor();
        }
    }

    static class ControlledExecutor implements Executor, AutoCloseable {
        private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable ->
                new Thread(runnable, "search-history-integration-worker"));
        private final AtomicInteger submitted = new AtomicInteger();
        private volatile CountDownLatch release = new CountDownLatch(0);
        private volatile CountDownLatch started = new CountDownLatch(0);

        @Override
        public void execute(Runnable command) {
            submitted.incrementAndGet();
            CountDownLatch taskRelease = release;
            CountDownLatch taskStarted = started;
            worker.execute(() -> {
                taskStarted.countDown();
                try {
                    if (!taskRelease.await(5, SECONDS)) {
                        throw new IllegalStateException("Async test task was not released");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                command.run();
            });
        }

        void pause() {
            release = new CountDownLatch(1);
            started = new CountDownLatch(1);
        }

        void awaitTaskStarted() throws InterruptedException {
            assertThat(started.await(5, SECONDS)).isTrue();
        }

        void awaitIdle() throws Exception {
            release.countDown();
            worker.submit(() -> { }).get(5, SECONDS);
        }

        int submittedCount() {
            return submitted.get();
        }

        void resetSubmittedCount() {
            submitted.set(0);
        }

        @Override
        public void close() {
            release.countDown();
            worker.shutdownNow();
        }
    }
}
