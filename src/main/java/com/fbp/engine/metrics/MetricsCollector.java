package com.fbp.engine.metrics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricsCollector {
    private static final MetricsCollector INSTANCE = new MetricsCollector();

    // REST API 조회를 위한 인메모리 저장소
    private final Map<String, NodeMetrics> metricsMap = new ConcurrentHashMap<>();

    // 1. 비동기 이벤트 큐 (Lock-free)
    private final ConcurrentLinkedQueue<MetricEvent> eventQueue = new ConcurrentLinkedQueue<>();

    // 2. 백그라운드 스케줄러 (10초마다 큐를 비워서 InfluxDB로 전송)
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 3. InfluxDB HTTP 클라이언트 및 설정
    private static final String INFLUX_URL = "http://localhost:8086/api/v2/write?org=iot-lab&bucket=fbp-metrics&precision=ms";
    private static final String INFLUX_TOKEN = "iot-lab-super-secret-auth-token"; // 발급받은 토큰 입력
    private final HttpClient httpClient;

    private MetricsCollector() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

        // 10초에 한 번씩 processEventsAndFlush() 메서드 실행
        scheduler.scheduleAtFixedRate(this::processEventsAndFlush, 10, 10, TimeUnit.SECONDS);
    }

    public static MetricsCollector getInstance() {
        return INSTANCE;
    }

    /**
     * Hot Path: 노드가 실행 스레드에서 호출 (매우 빠름)
     * 집계 연산을 하지 않고, 큐에 이벤트만 밀어넣은 뒤 즉시 리턴.
     */
    public void recordProcessing(String metricKey, long durationMs, boolean success) {
        eventQueue.offer(new MetricEvent(metricKey, durationMs, success));
    }

    /**
     * Cold Path: 백그라운드 스레드가 10초마다 실행
     * 큐에 쌓인 이벤트를 모아서 인메모리 맵을 업데이트하고 InfluxDB로 쏨
     */
    private void processEventsAndFlush() {
        int count = 0;
        MetricEvent event;
        StringBuilder lineProtocolBuilder = new StringBuilder();
        long timestampMs = System.currentTimeMillis();

        //  큐에서 이벤트를 하나씩 꺼냄
        while ((event = eventQueue.poll()) != null) {
            count++;

            // 기존 인메모리 로컬 맵 업데이트 (REST API용)
            NodeMetrics metrics = metricsMap.computeIfAbsent(event.key(), k -> new NodeMetrics());
            if (event.success()) {
                metrics.recordSuccess(event.durationMs());
            } else {
                metrics.recordError();
            }

            // B. InfluxDB 라인 프로토콜(Line Protocol) 텍스트 생성
            // metricKey가 "flow1:genNode" 형태이므로 쪼개서 InfluxDB 태그로 사용합니다.
            String[] parts = event.key().split(":");
            String flowId = parts.length > 1 ? parts[0] : "unknown";
            String nodeId = parts.length > 1 ? parts[1] : parts[0];

            // 형식: measurement,tag1=v1,tag2=v2 field1=v3,field2=v4 timestamp
            lineProtocolBuilder.append(String.format(
                    "node_execution,flow_id=%s,node_id=%s duration_ms=%di,is_success=%s %d\n",
                    flowId, nodeId, event.durationMs(), event.success(), timestampMs
            ));
        }

        // 모인 데이터가 있으면 한 방에 HTTP POST 전송 (Batch Write)
        if (count > 0) {
            log.info("[MetricsCollector] {}개의 메트릭 이벤트를 집계하여 InfluxDB로 전송합니다.", count);
            sendToInfluxDB(lineProtocolBuilder.toString());
        }
    }

    /**
     * InfluxDB로 실제 HTTP 통신을 수행합니다.
     */
    private void sendToInfluxDB(String lineProtocolData) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(INFLUX_URL))
                    .header("Authorization", "Token " + INFLUX_TOKEN)
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(lineProtocolData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("[MetricsCollector] InfluxDB 전송 실패 ({}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[MetricsCollector] InfluxDB HTTP 전송 중 에러 발생: {}", e.getMessage());
        }
    }


    public NodeMetrics getMetrics(String nodeId) {
        return metricsMap.get(nodeId);
    }

    public FlowMetrics getFlowMetrics(String flowId, List<String> nodeIds) {
        Map<String, NodeMetrics> flowNodes = new HashMap<>();
        for (String nodeId : nodeIds) {
            String metricKey = flowId + ":" + nodeId;
            flowNodes.put(nodeId, metricsMap.getOrDefault(metricKey, new NodeMetrics()));
        }
        return new FlowMetrics(flowId, flowNodes);
    }

    public void reset() {
        metricsMap.clear();
    }

    private record MetricEvent(String key, long durationMs, boolean success) {}
}