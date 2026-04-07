package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FilterNodeTest {

    @Test
    void testPassCondition() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 15.0);
        Message msg = new Message(data);

        filter.process(msg);
        assertEquals(msg, conn.poll());
    }

    @Test
    void testBlockCondition() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 5.0);
        Message msg = new Message(data);

        filter.process(msg);

        assertEquals(0, conn.getBufferSize());
    }

    @Test
    void testPortsNotNull() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        assertNotNull(filter.getInputPort("in"));
        assertNotNull(filter.getOutputPort("out"));
    }
}