package com.fbp.engine.engine.test;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class PerformanceResult {
    private final long totalMessages;       // 총 전송된 메시지 수
    private final long successCount;        // 성공적으로 끝까지 도달한 메시지 수
    private final long errorCount;          // 에러 발생 수
    private final long totalTimeMs;         // 전체 테스트 수행 시간 (ms)
    private final double throughputTps;     // 초당 처리량 (TPS - Transactions Per Second)
    private final double averageLatencyMs;  // 평균 지연 시간 (ms)
    private final long maxLatencyMs;        // 최대 지연 시간 (ms)

    /**
     * @return 기준을 통과했는지 여부를 boolean으로 반환 (TPS 1000 이상, 평균 지연 10ms 이하, 에러율 0.1% 미만)
     */
    public boolean isPassStandard() {
        double errorRate = (double) errorCount / totalMessages;
        return throughputTps >= 1000.0 && averageLatencyMs <= 10.0 && errorRate < 0.001;
    }
}