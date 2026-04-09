package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CounterNodeTest {

    @Test
    @DisplayName("count 키 추가")
    void testAddCountKey() throws InterruptedException {
        CounterNode counter = new CounterNode("counter");
        Connection conn = new Connection("c1");
        counter.getOutputPort("out").connect(conn);

        counter.initialize();
        counter.getInputPort("in").receive(new Message(new HashMap<>()));
        Thread.sleep(100);
        counter.shutdown();

        Message msg = conn.poll();
        assertNotNull(msg);
        assertEquals(1, (int) msg.get("count"));
    }

    @Test
    @DisplayName("count 누적")
    void testAccumulateCount() throws InterruptedException {
        CounterNode counter = new CounterNode("counter");
        Connection conn = new Connection("c1");
        counter.getOutputPort("out").connect(conn);

        counter.initialize();
        counter.getInputPort("in").receive(new Message(new HashMap<>()));
        counter.getInputPort("in").receive(new Message(new HashMap<>()));
        counter.getInputPort("in").receive(new Message(new HashMap<>()));
        Thread.sleep(100);
        counter.shutdown();

        conn.poll();
        conn.poll();
        Message msg3 = conn.poll();

        assertNotNull(msg3);
        assertEquals(3, (int) msg3.get("count"));
    }

    @Test
    @DisplayName("원본 키 유지")
    void testMaintainOriginalKeys() throws InterruptedException {
        CounterNode counter = new CounterNode("counter");
        Connection conn = new Connection("c1");
        counter.getOutputPort("out").connect(conn);

        counter.initialize();
        Map<String, Object> data = new HashMap<>();
        data.put("original", "value");
        counter.getInputPort("in").receive(new Message(data));
        Thread.sleep(100);
        counter.shutdown();

        Message result = conn.poll();
        assertNotNull(result);
        assertEquals("value", result.get("original"));
        assertNotNull(result.get("count"));
    }
}