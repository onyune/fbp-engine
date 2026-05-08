package com.fbp.engine.metrics;

import com.fbp.engine.message.Message;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DomainMetricsExtractor {
    private static final DomainMetricsExtractor INSTANCE = new DomainMetricsExtractor();
    
    // Key: "flowId:nodeId:portName" -> 해당 포트에서 뽑아낼 규칙 목록
    private final Map<String, List<DomainMetricRule>> rulesMap = new ConcurrentHashMap<>();

    private DomainMetricsExtractor() {}

    public static DomainMetricsExtractor getInstance() {
        return INSTANCE;
    }

    // 통계 추출 규칙 (어떤 노드의 어떤 포트에서, 무슨 필드를 뽑을 것인가?)
    public record DomainMetricRule(String sensorName, String sourceNode, String sourcePort, String field) {}

    // 외부에서 규칙을 등록하는 메서드
    public void addRule(String flowId, DomainMetricRule rule) {
        String key = flowId + ":" + rule.sourceNode() + ":" + rule.sourcePort();
        rulesMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(rule);
    }

    // 메시지가 지나갈 때 가로채서 값을 뽑아내는 메서드 (Hot Path에서 호출됨)
    public void extract(String flowId, String nodeId, String portName, Message message) {
        String key = flowId + ":" + nodeId + ":" + portName;
        List<DomainMetricRule> rules = rulesMap.get(key);
        
        // 이 포트에 등록된 규칙이 없거나 페이로드가 없으면 그냥 통과
        if (rules == null || rules.isEmpty() || message.getPayload() == null) return;

        Object rawPayload = message.getPayload();
        if (rawPayload instanceof Map<?, ?> payloadMap) {
            for (DomainMetricRule rule : rules) {
                Object fieldValue = payloadMap.get(rule.field());
                if (fieldValue != null) {
                    try {
                        // 값을 찾으면 숫자로 변환해서 수집기(Cold Path)로 던짐!
                        double numericValue = Double.parseDouble(fieldValue.toString());
                        MetricsCollector.getInstance().recordSensorRaw(
                            flowId, nodeId, rule.sensorName(), "dynamic_loc", numericValue
                        );
                    } catch (NumberFormatException ignore) {
                        // 숫자가 아니면 무시
                    }
                }
            }
        }
    }
}