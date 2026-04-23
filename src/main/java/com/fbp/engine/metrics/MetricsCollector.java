package com.fbp.engine.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// 노드/플로우 메트릭 수집 및 조회
/// 엔진 전체의 노드 통계를 맵으로 관리하는 싱글톤 객체
public class MetricsCollector {
    private static final MetricsCollector INSTANCE = new MetricsCollector();
    private final Map<String, NodeMetrics> metricsMap = new ConcurrentHashMap<>();

    private MetricsCollector() {
    }

    public static MetricsCollector getInstance(){
        return INSTANCE;
    }

    public void recordProcessing(String metricKey, long durationMs, boolean success){
        NodeMetrics metrics = metricsMap.computeIfAbsent(metricKey, k -> new NodeMetrics());
        if(success){
            metrics.recordSuccess(durationMs);
        }else{
            metrics.recordError();
        }
    }

    public NodeMetrics getMetrics(String nodeId){
        return metricsMap.get(nodeId);
    }

    public FlowMetrics getFlowMetrics(String flowId, List<String> nodeIds){
        Map<String, NodeMetrics> flowNodes = new HashMap<>();
        for(String nodeId : nodeIds){
            // ★ 핵심: 복합 키(flowId:nodeId)를 조합해서 꺼내옵니다!
            String metricKey = flowId + ":" + nodeId;
            flowNodes.put(nodeId, metricsMap.getOrDefault(metricKey, new NodeMetrics()));
        }
        return new FlowMetrics(flowId, flowNodes);
    }

    public void reset(){
        metricsMap.clear();
    }
}
