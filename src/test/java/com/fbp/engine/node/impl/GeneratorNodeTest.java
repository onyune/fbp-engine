package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneratorNodeTest {

    @Test
    @DisplayName("OutputPort 조회")
    void testOutputPortNotNull() {
        GeneratorNode gen = new GeneratorNode("gen");

        assertNotNull(gen.getOutputPort("out"));
    }

    @Test
    @DisplayName("generate 메시지 생성")
    void testGenerateMessage() {
        GeneratorNode gen = new GeneratorNode("gen");
        Connection conn = new Connection("c1");
        gen.getOutputPort("out").connect(conn);

        gen.generate("key", "value");

        assertEquals(1, conn.getBufferSize());
        assertNotNull(conn.poll());
    }

    @Test
    @DisplayName("메시지 내용 확인")
    void testMessageContent() {
        GeneratorNode gen = new GeneratorNode("gen");
        Connection conn = new Connection("c1");
        gen.getOutputPort("out").connect(conn);

        gen.generate("key1", "value1");

        Message msg = conn.poll();
        assertNotNull(msg);
        assertEquals("value1", msg.get("key1"));
    }

    @Test
    @DisplayName("다수 generate 호출")
    void testMultipleGenerates() {
        GeneratorNode gen = new GeneratorNode("gen");
        Connection conn = new Connection("c1");
        gen.getOutputPort("out").connect(conn);

        gen.generate("seq", 1);
        gen.generate("seq", 2);
        gen.generate("seq", 3);

        assertEquals(3, conn.getBufferSize());

        assertEquals(1, (int) conn.poll().get("seq"));
        assertEquals(2, (int) conn.poll().get("seq"));
        assertEquals(3, (int) conn.poll().get("seq"));
    }
}