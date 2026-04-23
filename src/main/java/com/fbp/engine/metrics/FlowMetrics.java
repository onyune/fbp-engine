package com.fbp.engine.metrics;

import java.util.Map;
import lombok.Getter;

@Getter
public class FlowMetrics {
    private final String flowId;
    private final long totalProcessed;
    private final long totalErrors;
    private final double overallAverageTime;

    private final Map<String, NodeMetrics> nodes;

    public FlowMetrics(String flowId, Map<String, NodeMetrics> nodes) {
        this.flowId = flowId;
        this.nodes = nodes;

        this.totalProcessed = nodes.values().stream()
                .mapToLong(NodeMetrics::getProcessedCount)
                .sum();
        this.totalErrors = nodes.values().stream()
                .mapToLong(NodeMetrics::getErrorCount)
                .sum();
        long totalTime = nodes.values().stream()
                .mapToLong(NodeMetrics::getTotalProcessingTime)
                .sum();

        this.overallAverageTime = totalProcessed == 0 ? 0.0 : (double) totalTime / totalProcessed;
    }
}
