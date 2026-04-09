package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilterNodeStep3Test {

    @Test
    @DisplayName("조건 만족 시 통과")
    void testPassCondition() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 15.0);
        Message msg = new Message(data);

        filter.process(msg);

        assertEquals(1, conn.getBufferSize());
        assertNotNull(conn.poll());
    }

    @Test
    @DisplayName("조건 미달 시 차단")
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
    @DisplayName("경계값 처리")
    void testBoundaryCondition() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("val", 10.0);
        Message msg = new Message(data);

        filter.process(msg);

        assertEquals(1, conn.getBufferSize());
    }

    @Test
    @DisplayName("키 없는 메시지")
    void testMissingKey() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        Connection conn = new Connection("c1");
        filter.getOutputPort("out").connect(conn);

        Map<String, Object> data = new HashMap<>();
        data.put("wrong_key", 15.0);
        Message msg = new Message(data);

        assertDoesNotThrow(() -> filter.process(msg));
        assertEquals(0, conn.getBufferSize());
    }

    @Test
    @DisplayName("포트 존재 여부 확인")
    void testPortsNotNull() {
        FilterNode filter = new FilterNode("filter", "val", 10.0);
        assertNotNull(filter.getInputPort("in"));
        assertNotNull(filter.getOutputPort("out"));
    }
}