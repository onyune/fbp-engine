package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

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