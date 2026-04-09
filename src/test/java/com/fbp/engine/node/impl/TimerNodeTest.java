package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimerNodeTest {

    @Test
    @DisplayName("initialize 후 메시지 생성 && tick 증가")
    void testInitializeAndTick() throws InterruptedException {
        TimerNode timer = new TimerNode("timer", 100);
        Connection conn = new Connection("c1");
        timer.getOutputPort("out").connect(conn);

        timer.initialize();
        Thread.sleep(350);
        timer.shutdown();

        int count = conn.getBufferSize();
        assertTrue(count >= 3 && count <= 5);

        for (int i = 0; i < count; i++) {
            Message msg = conn.poll();
            assertEquals(i, (Integer) msg.get("tick"));
        }
    }

    @Test
    @DisplayName("shutdown 후 정지")
    void testShutdown() throws InterruptedException {
        TimerNode timer = new TimerNode("timer", 100);
        Connection conn = new Connection("c1");
        timer.getOutputPort("out").connect(conn);

        timer.initialize();
        Thread.sleep(150);
        timer.shutdown();

        int countAfterShutdown = conn.getBufferSize();
        assertTrue(countAfterShutdown > 0);

        for (int i = 0; i < countAfterShutdown; i++) {
            conn.poll();
        }

        Thread.sleep(200);

        assertEquals(0, conn.getBufferSize());
    }

    @Test
    @DisplayName("주기 확인")
    void testIntervalFrequency() throws InterruptedException {
        TimerNode timer = new TimerNode("timer", 500);
        Connection conn = new Connection("c1");
        timer.getOutputPort("out").connect(conn);

        timer.initialize();
        Thread.sleep(2100);
        timer.shutdown();

        int count = conn.getBufferSize();

        assertTrue(count >= 3 && count <= 5);
    }
}