package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Test;

class GeneratorNodeTest {

    @Test
    void testGenerate() {
        GeneratorNode gen = new GeneratorNode("gen");

        assertNotNull(gen.getOutputPort("out"));

        Connection conn = new Connection("c1");
        gen.getOutputPort("out").connect(conn);

        gen.generate("key1", "value1");

        Message msg1 = conn.poll();
        assertNotNull(msg1);
        assertEquals("value1", msg1.get("key1"));

        gen.generate("key2", "value2");
        gen.generate("key3", "value3");

        Message msg2 = conn.poll();
        Message msg3 = conn.poll();

        assertNotNull(msg3);
        assertEquals("value3", msg3.get("key3"));
    }
}