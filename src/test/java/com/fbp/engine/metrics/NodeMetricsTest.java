package com.fbp.engine.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeMetricsTest {

    private NodeMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new NodeMetrics();
    }

    @Test
    @DisplayName("1. 초기값: 생성 직후 모든 카운터가 0")
    void testInitialValues() {
        assertEquals(0L, metrics.getProcessedCount());
        assertEquals(0L, metrics.getErrorCount());
        assertEquals(0L, metrics.getTotalProcessingTime());
        assertEquals(0.0, metrics.getAverageTime(), 0.001);
    }

    @Test
    @DisplayName("2. increment: 처리 건수, 에러 건수 증가")
    void testIncrement() {
        metrics.recordSuccess(100L);
        metrics.recordError();
        metrics.recordSuccess(50L);

        assertEquals(2L, metrics.getProcessedCount());
        assertEquals(1L, metrics.getErrorCount());
        assertEquals(150L, metrics.getTotalProcessingTime());
    }

    @Test
    @DisplayName("3. 평균 계산: 처리 시간 합계 / 처리 건수 = 평균")
    void testAverageCalculation() {
        metrics.recordSuccess(10L);
        metrics.recordSuccess(20L);
        metrics.recordSuccess(30L);

        assertEquals(3L, metrics.getProcessedCount());
        assertEquals(60L, metrics.getTotalProcessingTime());
        assertEquals(20.0, metrics.getAverageTime(), 0.001);
    }

    @Test
    @DisplayName("4. 스냅샷: 현재 메트릭의 불변 스냅샷 반환")
    void testSnapshot() {
        metrics.recordSuccess(10L);
        metrics.recordError();

        NodeMetrics.Snapshot snapshot = metrics.getSnapshot();

        metrics.recordSuccess(50L);
        metrics.recordError();

        assertEquals(1L, snapshot.processedCount());
        assertEquals(1L, snapshot.errorCount());
        assertEquals(10.0, snapshot.averageTime(), 0.001);

        assertNotEquals(snapshot.processedCount(), metrics.getProcessedCount());
        assertEquals(2L, metrics.getProcessedCount());
        assertEquals(2L, metrics.getErrorCount());
    }
}