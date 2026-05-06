package com.fbp.engine.engine.test;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MemoryMonitor {
    private final List<Long> usedMemoryRecords = new ArrayList<>();
    private ScheduledExecutorService scheduler;

    // 모니터링 시작
    public void start(long intervalMs) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::recordMemory, 0, intervalMs, TimeUnit.MILLISECONDS);
        log.info("[MemoryMonitor] 메모리 모니터링 시작 (주기: {}ms)", intervalMs);
    }

    // 모니터링 종료
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        log.info("[MemoryMonitor] 메모리 모니터링 종료. 총 {}회 기록됨.", usedMemoryRecords.size());
    }

    // 현재 메모리 기록
    private void recordMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        usedMemoryRecords.add(usedMemory);
    }

    /**
     * 메모리 사용량이 시간이 지남에 따라 지속적으로 우상향(단조 증가)하는지 검사합니다.
     * (가비지 컬렉터(GC)가 동작했음에도 메모리가 계속 늘어난다면 누수(Leak)로 판별)
     */
    public boolean isMonotonicallyIncreasing() {
        if (usedMemoryRecords.size() < 5) return false; // 데이터가 너무 적으면 판단 보류

        // 강제로 GC를 한번 돌려서 찌꺼기를 치움
        System.gc();
        recordMemory();

        int increaseCount = 0;
        for (int i = 1; i < usedMemoryRecords.size(); i++) {
            if (usedMemoryRecords.get(i) > usedMemoryRecords.get(i - 1)) {
                increaseCount++;
            }
        }
        // 전체 기록 중 90% 이상이 증가 추세라면 단조 증가(누수)로 판단
        double increaseRatio = (double) increaseCount / usedMemoryRecords.size();
        return increaseRatio > 0.9; 
    }
}