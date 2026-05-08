package com.fbp.engine.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetricsCollectorTest {

    private MetricsCollector collector;

    @BeforeEach
    void setUp() throws Exception {
        collector = MetricsCollector.getInstance();
        Field mapField = MetricsCollector.class.getDeclaredField("metricsMap");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(collector)).clear();
    }

    @Test
    @DisplayName("1. 처리 건수 기록: recordProcessing 호출 후 처리 건수 증가")
    void testRecordProcessingSuccess() {
        collector.recordNodeProcessing("flow-1", "node-1", "TestNode", 100L, true, 0, 0);

        NodeMetrics metrics = collector.getMetrics("flow-1", "node-1");
        assertNotNull(metrics);
        assertEquals(1L, metrics.getProcessedCount());
        assertEquals(0L, metrics.getErrorCount());
    }

    @Test
    @DisplayName("2. 에러 건수 기록: 실패로 기록 시 에러 카운트 증가")
    void testRecordProcessingError() {
        collector.recordNodeProcessing("flow-1", "node-2", "TestNode", 50L, false, 0, 0);

        NodeMetrics metrics = collector.getMetrics("flow-1", "node-2");
        assertNotNull(metrics);
        assertEquals(0L, metrics.getProcessedCount());
        assertEquals(1L, metrics.getErrorCount());
    }

    @Test
    @DisplayName("3. 평균 처리 시간: 여러 번 기록 후 평균 처리 시간 계산이 정확함")
    void testAverageProcessingTime() {
        collector.recordNodeProcessing("flow-1", "node-3", "TestNode", 10L, true, 0, 0);
        collector.recordNodeProcessing("flow-1", "node-3", "TestNode", 20L, true, 0, 0);
        collector.recordNodeProcessing("flow-1", "node-3", "TestNode", 30L, true, 0, 0);

        NodeMetrics metrics = collector.getMetrics("flow-1", "node-3");
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
                    collector.recordNodeProcessing("flow-1", "node-multi", "TestNode", 10L, true, 0, 0);
                }
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        NodeMetrics metrics = collector.getMetrics("flow-1", "node-multi");
        assertEquals(10000L, metrics.getProcessedCount());
    }

    @Test
    @DisplayName("5. 노드별 분리: 서로 다른 노드의 메트릭이 독립적으로 관리됨")
    void testNodeIsolation() {
        collector.recordNodeProcessing("flow-1", "node-A", "TestNode", 10L, true, 0, 0);
        collector.recordNodeProcessing("flow-1", "node-B", "TestNode", 20L, false, 0, 0);

        NodeMetrics metricsA = collector.getMetrics("flow-1", "node-A");
        NodeMetrics metricsB = collector.getMetrics("flow-1", "node-B");

        assertEquals(1L, metricsA.getProcessedCount());
        assertEquals(0L, metricsA.getErrorCount());

        assertEquals(0L, metricsB.getProcessedCount());
        assertEquals(1L, metricsB.getErrorCount());
    }

    @Test
    @DisplayName("6. 리셋: 메트릭 초기화 후 카운트가 0")
    void testReset() throws Exception {
        collector.recordNodeProcessing("flow-1", "node-reset", "TestNode", 100L, true, 0, 0);

        Field mapField = MetricsCollector.class.getDeclaredField("metricsMap");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(collector)).clear();

        NodeMetrics metrics = collector.getMetrics("flow-1", "node-reset");
        assertNotNull(metrics);
        assertEquals(0L, metrics.getProcessedCount());
    }

    @Test
    @DisplayName("7. 존재하지 않는 노드: 미등록 노드 id로 조회 시 빈 메트릭 반환")
    void testNonExistentNode() {
        NodeMetrics metrics = collector.getMetrics("flow-1", "unknown-node");
        assertNotNull(metrics);
        assertEquals(0L, metrics.getProcessedCount());
    }
}