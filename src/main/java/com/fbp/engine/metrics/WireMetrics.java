package com.fbp.engine.metrics;

import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;

@Getter
public class WireMetrics {
    private final String transport;
    private final LongAdder deliveredCount = new LongAdder();
    private final LongAdder droppedCount = new LongAdder();
    private volatile int currentQueueSize = 0;

    public WireMetrics(String transport) {
        this.transport = transport;
    }

    public void setCurrentQueueSize(int currentQueueSize) {
        this.currentQueueSize = currentQueueSize;
    }
}