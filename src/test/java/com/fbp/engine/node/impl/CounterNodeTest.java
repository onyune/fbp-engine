package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CounterNodeTest {

    @Test
    void testCounterIncrement() {
        CounterNode counter = new CounterNode("counter");
        Connection conn = new Connection("c1");
        counter.getOutputPort("out").connect(conn);

        counter.process(new Message(new HashMap<>()));
        Message msg1 = conn.poll();
        assertEquals(1, (Integer) msg1.get("count"));

        counter.process(new Message(new HashMap<>()));
        counter.process(new Message(new HashMap<>()));
        conn.poll(); // skip 2nd
        Message msg3 = conn.poll();
        assertEquals(3, (Integer) msg3.get("count"));
    }

    @Test
    void testMaintainOriginalKeys() {
        CounterNode counter = new CounterNode("counter");
        Connection conn = new Connection("c1");
        counter.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("original", "value");
        counter.process(new Message(data));

        Message result = conn.poll();
        assertEquals("value", result.get("original"));
        assertNotNull(result.get("count"));
    }
}

class DelayNodeTest {

    @Test
    void testDelayDelivery() throws InterruptedException {
        long delay = 500;
        DelayNode delayer = new DelayNode("delay", delay);
        Connection conn = new Connection("c1");
        delayer.getOutputPort("out").connect(conn);

        Message msg = new Message(new HashMap<>());
        long start = System.currentTimeMillis();

        delayer.process(msg);

        long end = System.currentTimeMillis();
        Message result = conn.poll();

        assertNotNull(result);
        assertTrue((end - start) >= delay);
    }

    @Test
    void testMessageContentPreserved() {
        DelayNode delayer = new DelayNode("delay", 100);
        Connection conn = new Connection("c1");
        delayer.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("key", "val");
        Message msg = new Message(data);

        delayer.process(msg);
        Message result = conn.poll();

        assertEquals(msg.getId(), result.getId());
        assertEquals("val", result.get("key"));
    }
}