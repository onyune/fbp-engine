package com.fbp.engine.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class AbstractNodeTest {

    static class DummyNode2 extends AbstractNode {
        boolean onProcessCalled = false;

        public DummyNode2(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        protected void onProcess(Message message) {
            onProcessCalled = true;
        }

        public void triggerSend(Message message) {
            send("out", message);
        }
    }

    @Test
    void testGetId() {
        DummyNode2 node = new DummyNode2("dummy-1");
        assertEquals("dummy-1", node.getId());
    }

    @Test
    void testAddInputPort() {
        DummyNode2 node = new DummyNode2("dummy");
        assertNotNull(node.getInputPort("in"));
    }

    @Test
    void testAddOutputPort() {
        DummyNode2 node = new DummyNode2("dummy");
        assertNotNull(node.getOutputPort("out"));
    }

    @Test
    void testUnknownPort() {
        DummyNode2 node = new DummyNode2("dummy");
        assertNull(node.getInputPort("unknown"));
    }

    @Test
    void testProcessCallsOnProcess() {
        DummyNode2 node = new DummyNode2("dummy");
        node.process(new Message(new HashMap<>()));
        assertTrue(node.onProcessCalled);
    }

    @Test
    void testSend() {
        DummyNode2 node = new DummyNode2("dummy");
        Connection conn = new Connection("c1");
        node.getOutputPort("out").connect(conn);
        Message msg = new Message(new HashMap<>());

        node.triggerSend(msg);
        assertEquals(msg, conn.poll());
    }
}