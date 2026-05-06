package com.fbp.engine.engine.test;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LoadTester {

    private final Node targetNode;          // 메시지를 주입할 시작 노드
    private final int totalMessages;        // 전송할 총 메시지 수
    
    // 지연 시간(Latency) 계산을 위한 기록 맵 (메시지 ID -> 생성 시간)
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalLatencyAccumulator = new AtomicLong(0);
    private final AtomicLong maxLatencyTracker = new AtomicLong(0);
    
    // 카운터 및 동기화 도구
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private CountDownLatch finishLatch;

    public LoadTester(Node targetNode, int totalMessages) {
        this.targetNode = targetNode;
        this.totalMessages = totalMessages;
        this.finishLatch = new CountDownLatch(totalMessages);
    }

    /**
     * 부하 테스트를 실행하고 결과를 반환합니다.
     */
    public PerformanceResult run() throws InterruptedException {
        log.info("[LoadTester] 부하 테스트 시작 총 {}건 발송 대기", totalMessages);
        
        long testStartTime = System.currentTimeMillis();

        // 메시지 생성 및 주입
        for (int i = 0; i < totalMessages; i++) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("testId", i);
            Message msg = new Message(payload);
            
            startTimes.put(msg.getId(), System.currentTimeMillis());
            
            // FBP 엔진의 시작 노드로 데이터 밀어넣기
            targetNode.process(msg);
        }

        // 모든 메시지가 플로우의 끝(종착 노드)에 도달할 때까지 대기
        finishLatch.await();
        
        long testEndTime = System.currentTimeMillis();
        long totalTimeMs = testEndTime - testStartTime;

        //지표 계산
        double throughputTps = (totalMessages / (double) totalTimeMs) * 1000;
        double averageLatencyMs = (double) totalLatencyAccumulator.get() / successCount.get();

        log.info("[LoadTester] 부하 테스트 완료. TPS: String.format(\"%.2f\", throughputTps), Avg Latency: String.format(\"%.2f\", averageLatencyMs)ms");

        return PerformanceResult.builder()
                .totalMessages(totalMessages)
                .successCount(successCount.get())
                .errorCount(errorCount.get())
                .totalTimeMs(totalTimeMs)
                .throughputTps(throughputTps)
                .averageLatencyMs(averageLatencyMs)
                .maxLatencyMs(maxLatencyTracker.get())
                .build();
    }

    /**
     * 플로우의 마지막을 장식하는 노드(예: CollectorNode)에서 호출해 주어야 하는 콜백 메서드.
     * 이를 통해 각 메시지의 종단 간(End-to-End) 지연 시간을 계산합니다.
     */
    public void recordCompletion(Message message, boolean isSuccess) {
        Long startTime = startTimes.remove(message.getId());
        if (startTime != null) {
            long latency = System.currentTimeMillis() - startTime;
            totalLatencyAccumulator.addAndGet(latency);
            
            // Max Latency 갱신 (Thread-safe)
            maxLatencyTracker.updateAndGet(currentMax -> Math.max(currentMax, latency));
        }

        if (isSuccess) {
            successCount.incrementAndGet();
        } else {
            errorCount.incrementAndGet();
        }
        
        // Latch 감소시켜 run()의 대기를 풀어줌
        finishLatch.countDown();
    }
}