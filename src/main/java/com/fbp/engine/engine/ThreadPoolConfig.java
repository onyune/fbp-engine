package com.fbp.engine.engine;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ToString
public class ThreadPoolConfig {

    private int corePoolSize;
    private int maxPoolSize;
    private long keepAliveTimeMs;
    private int queueCapacity;

    public ThreadPoolConfig() {
        int processors = Runtime.getRuntime().availableProcessors();
        this.corePoolSize = processors;
        this.maxPoolSize = processors * 2; // 코어 수의 2배를 최대치로 설정
        this.keepAliveTimeMs = 60000L;     // 유휴 스레드 60초 유지
        this.queueCapacity = 1000;         // 기본 큐 대기열 크기
    }

    @Builder
    public ThreadPoolConfig(int corePoolSize, int maxPoolSize, long keepAliveTimeMs, int queueCapacity) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTimeMs = keepAliveTimeMs;
        this.queueCapacity = queueCapacity;
    }

    /**
     * 현재 설정된 값을 바탕으로 최적화된 스레드 풀을 생성하여 반환합니다.
     */
    public ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTimeMs,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}