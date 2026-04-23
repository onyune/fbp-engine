package com.fbp.engine.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetricsCollectorTest {

    private MetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = MetricsCollector.getInstance();
        collector.reset();
    }

    @Test
    @DisplayName("1. 처리 건수 기록: recordProcessing 호출 후 처리 건수 증가")
    void testRecordProcessingSuccess() {
        collector.recordProcessing("node-1", 100L, true);

        NodeMetrics metrics = collector.getMetrics("node-1");
        assertNotNull(metrics);
        assertEquals(1L, metrics.getProcessedCount());
        assertEquals(0L, metrics.getErrorCount());
    }

    @Test
    @DisplayName("2. 에러 건수 기록: 실패로 기록 시 에러 카운트 증가")
    void testRecordProcessingError() {
        collector.recordProcessing("node-2", 50L, false);

        NodeMetrics metrics = collector.getMetrics("node-2");
        assertNotNull(metrics);
        assertEquals(0L, metrics.getProcessedCount());
        assertEquals(1L, metrics.getErrorCount());
    }

    @Test
    @DisplayName("3. 평균 처리 시간: 여러 번 기록 후 평균 처리 시간 계산이 정확함")
    void testAverageProcessingTime() {
        collector.recordProcessing("node-3", 10L, true);
        collector.recordProcessing("node-3", 20L, true);
        collector.recordProcessing("node-3", 30L, true);

        NodeMetrics metrics = collector.getMetrics("node-3");
        assertEquals(3L, metrics.getProcessedCount());
        assertEquals(20.0, metrics.getAverageTime(), 0.001);
    }

    @Test
    @DisplayName("4. 멀티스레드 안전성: 10개 스레드에서 동시에 기록해도 카운트가 정확함")
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int recordsPerThread = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < recordsPerThread; j++) {
                    collector.recordProcessing("node-multi", 10L, true);
                }
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        NodeMetrics metrics = collector.getMetrics("node-multi");
        assertEquals(10000L, metrics.getProcessedCount());
    }

    @Test
    @DisplayName("5. 노드별 분리: 서로 다른 노드의 메트릭이 독립적으로 관리됨")
    void testNodeIsolation() {
        collector.recordProcessing("node-A", 10L, true);
        collector.recordProcessing("node-B", 20L, false);

        NodeMetrics metricsA = collector.getMetrics("node-A");
        NodeMetrics metricsB = collector.getMetrics("node-B");

        assertEquals(1L, metricsA.getProcessedCount());
        assertEquals(0L, metricsA.getErrorCount());

        assertEquals(0L, metricsB.getProcessedCount());
        assertEquals(1L, metricsB.getErrorCount());
    }

    @Test
    @DisplayName("6. 리셋: 메트릭 초기화 후 카운트가 0")
    void testReset() {
        collector.recordProcessing("node-reset", 100L, true);
        collector.reset();

        assertNull(collector.getMetrics("node-reset"));
    }

    @Test
    @DisplayName("7. 존재하지 않는 노드: 미등록 노드 id로 조회 시 빈 메트릭 또는 null")
    void testNonExistentNode() {
        NodeMetrics metrics = collector.getMetrics("unknown-node");
        assertNull(metrics);
    }
}