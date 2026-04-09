package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThresholdFilterNodeTest {
    ThresholdFilterNode node;
    Connection alertConn;
    Connection normalConn;

    @BeforeEach
    void setUp(){
        node = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        alertConn = new Connection("alertConn");
        normalConn = new Connection("normalConn");

        node.getOutputPort("alert").connect(alertConn);
        node.getOutputPort("normal").connect(normalConn);
    }

    @Test
    @DisplayName("초과 → alert 포트")
    void test_alertPortForOverThreshold(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 35.5);
        Message msg = new Message(payload);

        node.onProcess(msg);

        assertEquals(1, alertConn.getBufferSize());
        assertEquals(0, normalConn.getBufferSize());
        assertNotNull(alertConn.poll());
    }

    @Test
    @DisplayName("이하 → normal 포트")
    void test_normalPortForUnderThreshold(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 25.0);
        Message msg = new Message(payload);

        node.onProcess(msg);

        assertEquals(0, alertConn.getBufferSize());
        assertEquals(1, normalConn.getBufferSize());
        assertNotNull(normalConn.poll());
    }

    @Test
    @DisplayName("경계값 (정확히 같은 값)")
    void test_boundaryValue(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 30.0);
        Message msg = new Message(payload);

        node.onProcess(msg);

        assertEquals(0, alertConn.getBufferSize());
        assertEquals(1, normalConn.getBufferSize());
        assertNotNull(normalConn.poll());
    }

    @Test
    @DisplayName("키 없는 메시지")
    void test_messageWithoutKey(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("humidity", 80.0);
        Message msg = new Message(payload);

        assertDoesNotThrow(()-> node.onProcess(msg));

        assertEquals(0, alertConn.getBufferSize());
        assertEquals(0, normalConn.getBufferSize());
    }

    @Test
    @DisplayName("양쪽 동시 검증 (CollectorNode 활용)")
    void test_simultaneousValidation() {
        ThresholdFilterNode node = new ThresholdFilterNode("filter-1", "temperature", 30.0);

        CollectorNode alertCollector = new CollectorNode("alertCollector");
        CollectorNode normalCollector = new CollectorNode("normalCollector");

        Connection alertConn = new Connection("alertConn");
        Connection normalConn = new Connection("normalConn");

        node.getOutputPort("alert").connect(alertConn);
        node.getOutputPort("normal").connect(normalConn);

        double[] testTemperatures = {35.0, 20.0, 40.0, 30.0};

        for(double temp : testTemperatures){
            Map<String, Object> payload = new HashMap<>();
            payload.put("temperature", temp);
            Message msg = new Message(payload);
            node.onProcess(msg);
        }

        while(alertConn.getBufferSize() > 0) {
            alertCollector.getInputPort("in").receive(alertConn.poll());
        }
        while(normalConn.getBufferSize() > 0) {
            normalCollector.getInputPort("in").receive(normalConn.poll());
        }

        assertEquals(2, alertCollector.getCollected().size());
        assertEquals(2, normalCollector.getCollected().size());

        for (Message msg : alertCollector.getCollected()) {
            assertTrue(((Number) msg.get("temperature")).doubleValue() > 30.0);
        }

        for (Message msg : normalCollector.getCollected()) {
            assertTrue(((Number) msg.get("temperature")).doubleValue() <= 30.0);
        }
    }
}