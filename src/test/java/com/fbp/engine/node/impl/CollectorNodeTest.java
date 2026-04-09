package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectorNodeTest {

    @Test
    @DisplayName("메시지 수집")
    void testCollectMessage() {
        CollectorNode collector = new CollectorNode("collector-1");
        Message msg = new Message(new HashMap<>());

        collector.onProcess(msg);

        List<Message> collected = collector.getCollected();
        assertEquals(1, collected.size());
        assertEquals(msg, collected.get(0));
    }

    @Test
    @DisplayName("수집 순서 보존")
    void testPreserveOrder() {
        CollectorNode collector = new CollectorNode("collector-1");
        Message msg1 = new Message(new HashMap<>(Map.of("seq", 1)));
        Message msg2 = new Message(new HashMap<>(Map.of("seq", 2)));
        Message msg3 = new Message(new HashMap<>(Map.of("seq", 3)));

        collector.onProcess(msg1);
        collector.onProcess(msg2);
        collector.onProcess(msg3);

        List<Message> collected = collector.getCollected();
        assertEquals(3, collected.size());
        assertEquals(1, (int)collected.get(0).get("seq"));
        assertEquals(2, (int)collected.get(1).get("seq"));
        assertEquals(3, (int)collected.get(2).get("seq"));
    }

    @Test
    @DisplayName("초기 상태 빈 리스트")
    void testInitialEmptyList() {
        CollectorNode collector = new CollectorNode("collector-1");

        assertNotNull(collector.getCollected());
        assertTrue(collector.getCollected().isEmpty());
    }

    @Test
    @DisplayName("InputPort 존재")
    void testInputPortExists() {
        CollectorNode collector = new CollectorNode("collector-1");

        assertNotNull(collector.getInputPort("in"));
    }

    @Test
    @DisplayName("파이프라인 연결 검증")
    void testPipelineConnection() {
        GeneratorNode generator = new GeneratorNode("gen-1");
        CollectorNode collector = new CollectorNode("collector-1");
        Connection conn = new Connection("testConn");

        generator.getOutputPort("out").connect(conn);

        generator.generate("testKey", "First Data");
        generator.generate("testKey", "Second Data");

        while(conn.getBufferSize() > 0) {
            collector.getInputPort("in").receive(conn.poll());
        }

        List<Message> collected = collector.getCollected();
        assertEquals(2, collected.size());
        assertEquals("First Data", collected.get(0).get("testKey"));
        assertEquals("Second Data", collected.get(1).get("testKey"));
    }
}