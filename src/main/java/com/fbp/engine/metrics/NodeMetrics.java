package com.fbp.engine.metrics;

import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.ConcurrentHistogram;

public class NodeMetrics {
    // 병목(Contention) 방지를 위해 LongAdder 사용
    private final LongAdder processedCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    private final LongAdder totalProcessingTime = new LongAdder();


    private final ConcurrentHistogram latencyHistogram = new ConcurrentHistogram(1, 3600000, 3);
    public record Snapshot(long processedCount, long errorCount, double averageTime) {}

    private final LongAdder inCount = new LongAdder();
    private final LongAdder outCount = new LongAdder();
    private final LongAdder inBytes = new LongAdder();
    private final LongAdder outBytes = new LongAdder();

    public void recordProcessing(long timeMs, boolean success, int inB, int outB) {
        inCount.increment();
        inBytes.add(inB);
        if (success) {
            processedCount.increment();
            outCount.increment();
            outBytes.add(outB);
            totalProcessingTime.add(timeMs);
            latencyHistogram.recordValue(timeMs);
        } else {
            errorCount.increment();
        }
    }

    public long getInCount() { return inCount.sum(); }
    public long getOutCount() { return outCount.sum(); }
    public long getInBytes() { return inBytes.sum(); }
    public long getOutBytes() { return outBytes.sum(); }

    public long getProcessedCount() { return processedCount.sum(); }
    public long getErrorCount() { return errorCount.sum(); }
    public long getTotalProcessingTime() { return totalProcessingTime.sum(); }

    public double getAverageTime() {
        long count = getProcessedCount();
        return count == 0 ? 0.0 : (double) getTotalProcessingTime() / count;
    }
    public long getP99Time() {
        return latencyHistogram.getTotalCount() > 0 ? latencyHistogram.getValueAtPercentile(99.0) : 0;
    }

    public Snapshot getSnapshot() {
        return new Snapshot(getProcessedCount(), getErrorCount(), getAverageTime());
    }
}