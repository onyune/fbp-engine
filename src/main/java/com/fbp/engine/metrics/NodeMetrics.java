package com.fbp.engine.metrics;

import java.util.concurrent.atomic.AtomicLong;

/// 노드별 메트릭 데이터 (처리 건수, 에러 수, 평균 처리시간)
public class NodeMetrics {
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    public record Snapshot(long processedCount, long errorCount, double averageTime) {}

    public void recordSuccess(long timeMs) {
        processedCount.incrementAndGet();
        totalProcessingTime.addAndGet(timeMs);
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }
    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    public double getAverageTime() {
        long count = processedCount.get();

        return count == 0 ? 0.0 : (double) totalProcessingTime.get() / count;
    }

    public Snapshot getSnapshot() {
        return new Snapshot(getProcessedCount(), getErrorCount(), getAverageTime());
    }
}
