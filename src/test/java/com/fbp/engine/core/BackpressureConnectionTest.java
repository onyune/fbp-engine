package com.fbp.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fbp.engine.core.impl.BlockStrategy;
import com.fbp.engine.core.impl.DropNewestStrategy;
import com.fbp.engine.core.impl.DropOldestStrategy;
import com.fbp.engine.message.Message;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BackpressureConnectionTest {

    @Test
    @DisplayName("1. Block 전략: 큐 가득 참 → send()가 블로킹됨 (타임아웃으로 확인)")
    void testBlockStrategyTimeout() throws InterruptedException {
        BackpressureConnection conn = new BackpressureConnection("c1", 1, new BlockStrategy());
        conn.send(new Message(Map.of("id", "1")));

        CountDownLatch latch = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            conn.send(new Message(Map.of("id", "2")));
            latch.countDown();
        });

        producer.start();
        boolean finished = latch.await(500, TimeUnit.MILLISECONDS);

        assertFalse(finished, "Block 전략일 때 큐가 가득 차면 send()는 대기해야 합니다.");
        producer.interrupt();
    }

    @Test
    @DisplayName("2. DropOldest 전략: 큐 가득 참 + 새 메시지 → 가장 오래된 메시지 제거 확인")
    void testDropOldest() {
        BackpressureConnection conn = new BackpressureConnection("c1", 2, new DropOldestStrategy());
        conn.send(new Message(Map.of("v", "A")));
        conn.send(new Message(Map.of("v", "B")));

        conn.send(new Message(Map.of("v", "C")));

        assertEquals(2, conn.getQueue().size());
        assertEquals("B", conn.getQueue().poll().getPayload().get("v"));
        assertEquals("C", conn.getQueue().poll().getPayload().get("v"));
    }

    @Test
    @DisplayName("3. DropNewest 전략: 큐 가득 참 + 새 메시지 → 새 메시지가 버려짐")
    void testDropNewest() {
        BackpressureConnection conn = new BackpressureConnection("c1", 2, new DropNewestStrategy());
        conn.send(new Message(Map.of("v", "A")));
        conn.send(new Message(Map.of("v", "B")));

        conn.send(new Message(Map.of("v", "C")));

        assertEquals(2, conn.getQueue().size());
        assertEquals("A", conn.getQueue().poll().getPayload().get("v"));
        assertEquals("B", conn.getQueue().poll().getPayload().get("v"));
    }

    @Test
    @DisplayName("4. 전략 변경: 런타임에 전략 변경 후 새 전략이 적용됨")
    void testStrategyChange() {
        BackpressureConnection conn = new BackpressureConnection("c1", 1, new DropNewestStrategy());
        conn.send(new Message(Map.of("v", "1")));

        // 전략을 DropOldest로 변경
        conn.setStrategy(new DropOldestStrategy());
        conn.send(new Message(Map.of("v", "2")));

        assertEquals("2", conn.getQueue().poll().getPayload().get("v"));
    }

    @Test
    @DisplayName("5. 큐 크기 설정: 생성 시 지정한 큐 용량이 적용됨")
    void testQueueCapacity() {
        BackpressureConnection conn = new BackpressureConnection("c1", 5, new DropNewestStrategy());
        for(int i=0; i<10; i++) conn.send(new Message(Map.of("v", i)));

        assertEquals(5, conn.getQueue().size(), "용량이 5이므로 그 이상은 담길 수 없습니다.");
    }

    @Test
    @DisplayName("6. 드롭 카운트: Drop 전략에서 드롭된 메시지 수 메트릭 확인")
    void testDropCountMetric() {
        BackpressureConnection conn = new BackpressureConnection("c1", 2, new DropNewestStrategy());
        conn.send(new Message(Map.of("v", "1")));
        conn.send(new Message(Map.of("v", "2")));
        conn.send(new Message(Map.of("v", "3")));
        conn.send(new Message(Map.of("v", "4")));

        assertEquals(2, conn.getDropCount().get(), "총 2개의 메시지가 드롭되어야 합니다.");
    }

    @Test
    @DisplayName("7. 멀티스레드: 여러 생산자 스레드에서 동시 전송 시 데이터 손실 없음(정합성)")
    void testMultiThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int msgPerThread = 100;
        BackpressureConnection conn = new BackpressureConnection("c1", 1000, new BlockStrategy());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < msgPerThread; j++) {
                    conn.send(new Message(Map.of("d", "data")));
                }
                latch.countDown();
            });
        }

        latch.await(2, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount * msgPerThread, conn.getQueue().size(), "모든 스레드가 보낸 메시지가 큐에 정확히 쌓여야 합니다.");
    }
}