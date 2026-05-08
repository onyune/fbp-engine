package com.fbp.engine.metrics;

import java.util.concurrent.atomic.LongAdder;

public class WireMetrics {
        final String transport;
        final LongAdder deliveredCount = new LongAdder();
        final LongAdder droppedCount = new LongAdder();
        volatile int currentQueueSize = 0;

        public WireMetrics(String transport) {
            this.transport = transport;
        }
    }