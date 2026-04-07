package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransformNodeTest {

    @Test
    void testTransformSuccess() {
        TransformNode transformer = new TransformNode("trans", msg -> {
            Integer val = msg.get("val");
            return msg.withEntry("val", val * 2);
        });
        Connection conn = new Connection("c1");
        transformer.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 10);
        Message msg = new Message(data);

        transformer.process(msg);

        Message result = conn.poll();
        assertNotNull(result);
        assertEquals(20, (Integer) result.get("val"));
    }

    @Test
    void testTransformNullReturnsNothing() {
        TransformNode transformer = new TransformNode("trans", msg -> null);
        Connection conn = new Connection("c1");
        transformer.getOutputPort("out").connect(conn);

        transformer.process(new Message(new HashMap<>()));
        assertEquals(0, conn.getBufferSize());
    }

    @Test
    void testOriginalMessageImmutability() {
        TransformNode transformer = new TransformNode("trans", msg -> msg.withEntry("newKey", "newVal"));

        Map<String, Object> data = new HashMap<>();
        data.put("oldKey", "oldVal");
        Message original = new Message(data);

        transformer.process(original);

        assertFalse(original.hasKey("newKey"));
        assertEquals("oldVal", original.get("oldKey"));
    }
}