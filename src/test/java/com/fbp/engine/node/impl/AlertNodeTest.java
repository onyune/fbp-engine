package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertNodeTest {
    AlertNode node;

    @BeforeEach
    void setUp(){
        node = new AlertNode("alert-1");
    }

    @Test
    @DisplayName("정상 처리 ")
    void test_normalProcessing(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("sensorId", "sensor-A");
        payload.put("temperature", 35.5);
        Message msg = new Message(payload);

        assertDoesNotThrow(()-> node.onProcess(msg) );
    }

    @Test
    @DisplayName("키 누락 시 처리")
void test_missingKeyProcessing(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("sensorId", "sensor-B");
        Message msg = new Message(payload);
        assertDoesNotThrow(()-> node.onProcess(msg) );

    }

}