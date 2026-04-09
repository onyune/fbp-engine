package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilterNodeTestStep5 {

    @Test
    @DisplayName("조건 만족 시 통과 (초과 및 경계값)")
    void testPassCondition() throws InterruptedException {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        filter.initialize();

        filter.getInputPort("in").receive(new Message(new HashMap<>(Map.of("val", 15.0))));
        filter.getInputPort("in").receive(new Message(new HashMap<>(Map.of("val", 10.0))));

        Thread.sleep(100);
        filter.shutdown();

        assertEquals(2, conn.getBufferSize());
        assertEquals(15.0, (double) conn.poll().get("val"));
        assertEquals(10.0, (double) conn.poll().get("val"));
    }

    @Test
    @DisplayName("조건 미달 시 차단")
    void testBlockCondition() throws InterruptedException {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        filter.initialize();

        filter.getInputPort("in").receive(new Message(new HashMap<>(Map.of("val", 5.0))));

        Thread.sleep(100);
        filter.shutdown();

        assertEquals(0, conn.getBufferSize());
    }

    @Test
    @DisplayName("키 없는 메시지 무시")
    void testMissingKey() throws InterruptedException {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        filter.initialize();

        assertDoesNotThrow(() -> {
            filter.getInputPort("in").receive(new Message(new HashMap<>(Map.of("wrong_key", 15.0))));
        });

        Thread.sleep(100);
        filter.shutdown();

        assertEquals(0, conn.getBufferSize());
    }
}