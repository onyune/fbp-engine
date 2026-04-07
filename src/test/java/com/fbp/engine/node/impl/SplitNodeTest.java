package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SplitNodeTest {

    @Test
    void testSplitMatch() {
        SplitNode splitter = new SplitNode("split", "val", 10.0);
        Connection matchConn = new Connection("match");
        splitter.getOutputPort("match").connect(matchConn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 15.0);
        splitter.process(new Message(data));

        assertNotNull(matchConn.poll());
    }

    @Test
    void testSplitMismatch() {
        SplitNode splitter = new SplitNode("split", "val", 10.0);
        Connection mismatchConn = new Connection("mismatch");
        splitter.getOutputPort("mismatch").connect(mismatchConn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 5.0);
        splitter.process(new Message(data));

        assertNotNull(mismatchConn.poll());
    }

    @Test
    void testSplitBothDirections() {
        SplitNode splitter = new SplitNode("split", "val", 10.0);
        Connection matchConn = new Connection("m1");
        Connection mismatchConn = new Connection("m2");
        splitter.getOutputPort("match").connect(matchConn);
        splitter.getOutputPort("mismatch").connect(mismatchConn);

        Map<String, Object> d1 = new HashMap<>(); d1.put("val", 15.0);
        Map<String, Object> d2 = new HashMap<>(); d2.put("val", 5.0);

        splitter.process(new Message(d1));
        splitter.process(new Message(d2));

        assertEquals(1, matchConn.getBufferSize());
        assertEquals(1, mismatchConn.getBufferSize());
    }

    @Test
    void testBoundaryValue() {
        SplitNode splitter = new SplitNode("split", "val", 10.0);
        Connection matchConn = new Connection("m1");
        splitter.getOutputPort("match").connect(matchConn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 10.0);
        splitter.process(new Message(data));

        assertNotNull(matchConn.poll());
    }
}