package com.fbp.engine.metrics;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

@Slf4j
public class MetricsCollector {
    private static final MetricsCollector INSTANCE = new MetricsCollector();

    // 성능 보호: 큐 크기 제한 (Drop on overflow)
    private static final int MAX_QUEUE_SIZE = 10000;
    private final ArrayBlockingQueue<String> eventQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    // 장애 대비 로컬 버퍼: DB 장애 시 임시 저장 (최대 5만 건)
    private static final int MAX_OFFLINE_BUFFER = 50000;
    private final LinkedList<String> offlineBuffer = new LinkedList<>();

    // 통계 캐시 및 스케줄러
    private final Map<String, NodeMetrics> metricsMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final LongAdder droppedMetricsCount = new LongAdder(); // 큐 가득 차서 버린 메트릭 수

    // 엔진 전체 통계용
    private final LongAdder globalProcessed = new LongAdder();
    private final LongAdder globalErrors = new LongAdder();

    // 연결(Wire) 통계 캐시용
    private final Map<String, WireMetrics> wireMetricsMap = new ConcurrentHashMap<>();

    // InfluxDB 설정
    private String influxUrl;
    private String influxToken;
    private int batchSize;
    private int maxOfflineBuffer;
    private final HttpClient httpClient;

    private final Map<String, SensorWindow> window1m = new ConcurrentHashMap<>();
    private final Map<String, SensorWindow> window1h = new ConcurrentHashMap<>();
    private final Map<String, SensorWindow> window1d = new ConcurrentHashMap<>();
    private final Map<String, FlowMetricsCache> flowMetricsMap = new ConcurrentHashMap<>();

    private MetricsCollector() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

        loadConfiguration();
        long flushIntervalSec = (long) (batchSize == 0 ? 10 : 10); // 기본 10초
        // 10초 주기 Tick 발생
        scheduler.scheduleAtFixedRate(this::processEventsAndFlush, flushIntervalSec, flushIntervalSec, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> flushSensorWindow(window1m, "sensor_stats_1m"), 1, 1, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(() -> flushSensorWindow(window1h, "sensor_stats_1h"), 1, 1, TimeUnit.HOURS);
        scheduler.scheduleAtFixedRate(() -> flushSensorWindow(window1d, "sensor_stats_1d"), 1, 1, TimeUnit.DAYS);
    }

    private void loadConfiguration() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            if (inputStream == null) {
                log.warn("application.yml 파일을 찾을 수 없습니다. 기본값을 사용합니다.");
                setDefaults();
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            Map<String, Object> influx = (Map<String, Object>) config.get("influxdb");

            // URL 조합 (API 엔드포인트 포함)
            String rawUrl = (String) influx.get("url");
            String org = (String) influx.get("org");
            String bucket = (String) influx.get("bucket");
            this.influxUrl = String.format("%s/api/v2/write?org=%s&bucket=%s&precision=ms", rawUrl, org, bucket);

            // 토큰 환경변수 치환 로직 ( ${INFLUX_TOKEN} 이면 환경변수에서 읽고, 아니면 그냥 씀 )
            String rawToken = (String) influx.get("token");
            if (rawToken != null && rawToken.startsWith("${") && rawToken.endsWith("}")) {
                String envVarName = rawToken.substring(2, rawToken.length() - 1);
                this.influxToken = System.getenv(envVarName) != null ? System.getenv(envVarName) : "default-token";
            } else {
                this.influxToken = rawToken;
            }

            // 배치 및 버퍼 설정 읽기
            Map<String, Object> batch = (Map<String, Object>) influx.get("batch");
            this.batchSize = (Integer) batch.get("size");

            Map<String, Object> buffer = (Map<String, Object>) influx.get("buffer");
            this.maxOfflineBuffer = (Integer) buffer.get("max_size");

            log.info("MetricsCollector InfluxDB 설정 로드 완료 (버퍼 크기: {})", maxOfflineBuffer);

        } catch (Exception e) {
            log.error("YAML 설정 로드 중 오류 발생. 기본값을 사용합니다.", e);
            setDefaults();
        }
    }

    private void setDefaults() {
        this.influxUrl = "http://localhost:8086/api/v2/write?org=iot-lab&bucket=fbp-metrics&precision=ms";
        this.influxToken = "iot-lab-super-secret-auth-token";
        this.batchSize = 1000;
        this.maxOfflineBuffer = 100000;
    }

    public static MetricsCollector getInstance() {
        return INSTANCE;
    }

    /**
     * [노드 통계 기록] Hot Path - 절대 블로킹되면 안 됨!
     */
    public void recordNodeProcessing(String flowId, String nodeId, String nodeType, long durationMs, boolean success, int inBytes, int outBytes) {
        long timestampMs = System.currentTimeMillis();

        // 메모리 캐시 업데이트 (LongAdder 사용)
        String metricKey = flowId + ":" + nodeId;
        NodeMetrics metrics = metricsMap.computeIfAbsent(metricKey, k -> new NodeMetrics());
        if (success) {
            metrics.recordSuccess(durationMs);
            globalProcessed.increment();
        }
        else{
            metrics.recordError();
            globalErrors.increment();
        }

        // Line Protocol 생성
        String lineProtocol = String.format(
                "node_stats,engine_id=local-engine,flow_id=%s,node_id=%s,node_type=%s " +
                        "in_count=1i,out_count=%di,in_bytes=%di,out_bytes=%di,avg_time_ms=%f,p99_time_ms=%di,errors=%di %d",
                flowId, nodeId, nodeType,
                (success ? 1 : 0), inBytes, outBytes,
                metrics.getAverageTime(), // 실시간 누적 평균
                metrics.getP99Time(),     // 실시간 상위 1% 지연 시간 (HdrHistogram)
                (success ? 0 : 1), timestampMs
        );

        // 비동기 큐에 삽입 (꽉 찼으면 즉시 버림 - Drop on overflow)
        if (!eventQueue.offer(lineProtocol)) {
            droppedMetricsCount.increment();
        }
    }

    /**
     * [도메인(센서) 통계 기록] - 센서 원천 데이터
     */
    public void recordSensorRaw(String flowId, String nodeId, String sensorName, String location, double value) {
        String lineProtocol = String.format(
                "sensor_raw,engine_id=local-engine,flow_id=%s,node_id=%s,sensor_name=%s,location=%s value=%f %d",
                flowId, nodeId, sensorName, location, value, System.currentTimeMillis()
        );
        if (!eventQueue.offer(lineProtocol)) droppedMetricsCount.increment();

        String windowKey = sensorName + ":" + location;
        window1m.computeIfAbsent(windowKey, k -> new SensorWindow()).add(value);
        window1h.computeIfAbsent(windowKey, k -> new SensorWindow()).add(value);
        window1d.computeIfAbsent(windowKey, k -> new SensorWindow()).add(value);
    }

    /**
     * Cold Path: 10초마다 큐를 비워서 InfluxDB로 Batch 전송
     */
    private void processEventsAndFlush() {
        if (droppedMetricsCount.sum() > 0) {
            log.warn("[MetricsCollector] 큐 초과로 버려진 메트릭 수: {}", droppedMetricsCount.sumThenReset());
        }

        StringBuilder batchPayload = new StringBuilder();
        int count = 0;
        long timestampMs = System.currentTimeMillis();
        long heapUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        batchPayload.append(String.format(
                "engine_stats,engine_id=local-engine heap_used=%di,total_processed=%di,total_errors=%di %d%n",
                heapUsed, globalProcessed.sum(), globalErrors.sum(), timestampMs
        ));
        for (Map.Entry<String, WireMetrics> entry : wireMetricsMap.entrySet()) {
            WireMetrics wm = entry.getValue();
            batchPayload.append(String.format(
                    "wire_stats,engine_id=local-engine,wire_id=%s,transport=%s delivered=%di,dropped=%di,queue_size=%di %d\n",
                    entry.getKey(), wm.transport, wm.deliveredCount.sumThenReset(), wm.droppedCount.sumThenReset(), wm.currentQueueSize, timestampMs
            ));
        }

        for (Map.Entry<String, FlowMetricsCache> entry : flowMetricsMap.entrySet()) {
            FlowMetricsCache fmc = entry.getValue();
            long processed = fmc.processed.sum();
            double throughput = processed / 10.0; // 10초 기준 초당 처리량
            double avgLatency = processed == 0 ? 0 : (double) fmc.totalLatency.sum() / processed;

            batchPayload.append(String.format(
                    "flow_stats,engine_id=local-engine,flow_id=%s,transport=%s processed=%di,errors=%di,throughput=%f,avg_latency_ms=%f %d\n",
                    entry.getKey(), fmc.transport, fmc.processed.sumThenReset(), fmc.errors.sumThenReset(), throughput, avgLatency, System.currentTimeMillis()
            ));
        }

        // 오프라인 버퍼에 남은 게 있다면 먼저 묶음
        while (!offlineBuffer.isEmpty() && count < 2000) {
            batchPayload.append(offlineBuffer.removeFirst()).append("\n");
            count++;
        }

        // 현재 큐에 있는 이벤트 묶음
        String event;
        while ((event = eventQueue.poll()) != null && count < 5000) {
            batchPayload.append(event).append("\n");
            count++;
        }

        //  InfluxDB 전송
        if (count > 0) {
            boolean success = sendToInfluxDB(batchPayload.toString());

            //  전송 실패 시 로컬 버퍼에 저장 (장애 복구)
            if (!success) {
                log.warn("InfluxDB 전송 실패. {}건의 메트릭을 로컬 버퍼에 저장합니다.", count);
                String[] failedEvents = batchPayload.toString().split("\n");
                for (String ev : failedEvents) {
                    if (ev.isEmpty()) continue;
                    // 버퍼가 꽉 차면 가장 오래된 데이터를 버림 (Drop Oldest)
                    if (offlineBuffer.size() >= MAX_OFFLINE_BUFFER) {
                        offlineBuffer.removeFirst();
                    }
                    offlineBuffer.addLast(ev);
                }
            } else {
                log.info("[MetricsCollector] {}건 메트릭 InfluxDB 적재 완료", count);
            }
        }
    }

    public NodeMetrics getMetrics(String flowId, String nodeId) {
        // 복합 키로 조회하고, 없으면 빈(0) 통계 객체 반환
        return metricsMap.getOrDefault(flowId + ":" + nodeId, new NodeMetrics());
    }

    public FlowMetrics getFlowMetrics(String flowId, List<String> nodeIds) {
        Map<String, NodeMetrics> flowNodes = new HashMap<>();
        for (String nodeId : nodeIds) {
            flowNodes.put(nodeId, getMetrics(flowId, nodeId));
        }
        return new FlowMetrics(flowId, flowNodes);
    }

    /**
     * Connection에서 보내는 보고를 받는 메서드
     * @param wireId
     * @param transport
     * @param success
     * @param queueSize
     */
    public void recordWireEvent(String wireId, String transport, boolean success, int queueSize) {
        WireMetrics wm = wireMetricsMap.computeIfAbsent(wireId, k -> new WireMetrics(transport));
        if (success) {
            wm.deliveredCount.increment();
        } else {
            wm.droppedCount.increment(); // 큐가 꽉 차서 버려짐!
        }
        wm.currentQueueSize = queueSize; // 최신 큐 사이즈 갱신
    }

    public void recordFlowStats(String flowId, String transport, long durationMs, boolean success) {
        FlowMetricsCache fmc = flowMetricsMap.computeIfAbsent(flowId, k -> new FlowMetricsCache(transport));
        if (success) {
            fmc.processed.increment();
            fmc.totalLatency.add(durationMs);
        } else {
            fmc.errors.increment();
        }
    }
    public void recordFlowEvent(String flowId, String eventType, String user, String summary) {
        String lineProtocol = String.format(
                "flow_events,engine_id=local-engine,flow_id=%s,event_type=%s,user=%s change_summary=\"%s\" %d",
                flowId, eventType, user, summary, System.currentTimeMillis()
        );
        eventQueue.offer(lineProtocol);
    }

    private boolean sendToInfluxDB(String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(influxUrl))
                    .header("Authorization", "Token " + influxToken)
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    // 바구니를 비우고 통계를 계산하여 전송하는 메서드
    // 범용 윈도우 플러시 메서드 (1m, 1h, 1d 공용)
    private void flushSensorWindow(Map<String, SensorWindow> targetWindow, String measurementName) {
        if (targetWindow.isEmpty()) return;

        long timestampMs = System.currentTimeMillis();
        StringBuilder batch = new StringBuilder();

        Map<String, SensorWindow> snapshot;
        synchronized (targetWindow) {
            snapshot = new HashMap<>(targetWindow);
            targetWindow.clear();
        }

        for (Map.Entry<String, SensorWindow> entry : snapshot.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String sensorName = parts[0];
            String location = parts.length > 1 ? parts[1] : "unknown";
            SensorWindow win = entry.getValue();

            if (win.count > 0) {
                double avg = win.sum / win.count;
                batch.append(String.format(
                        "%s,engine_id=local-engine,sensor_name=%s,location=%s avg=%f,min=%f,max=%f,count=%di %d\n",
                        measurementName, sensorName, location, avg, win.min, win.max, win.count, timestampMs
                ));
            }
        }

        if (batch.length() > 0) {
            sendToInfluxDB(batch.toString());
        }
    }

    //바구니 역할을 하는 내부 클래스 (최대, 최소, 합계, 개수 추적)
    public static class SensorWindow {
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int count = 0;

        public synchronized void add(double value) {
            sum += value;
            count++;
            if (value < min) min = value;
            if (value > max) max = value;
        }
    }

    public static class FlowMetricsCache {
        final String transport;
        final LongAdder processed = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder totalLatency = new LongAdder();

        public FlowMetricsCache(String transport) { this.transport = transport; }
    }
}