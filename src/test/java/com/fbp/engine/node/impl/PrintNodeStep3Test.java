package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PrintNodeStep3Test {
    @Test
    @DisplayName("InputPort 존재 확인")
    void testInputPortExists() {
        PrintNode printNode = new PrintNode("print-1");

        assertNotNull(printNode.getInputPort("in"));
    }

    @Test
    @DisplayName("정상 메시지 처리")
    void testNormalProcessing() {
        PrintNode printNode = new PrintNode("print-1");
        Map<String, Object> data = new HashMap<>();
        data.put("testKey", "testValue");
        Message msg = new Message(data);

        assertDoesNotThrow(() -> printNode.process(msg));
    }

    @Test
    @DisplayName("빈 메시지 예외 없음")
    void testEmptyMessageProcessing() {
        PrintNode printNode = new PrintNode("print-1");
        Message msg = new Message(new HashMap<>());

        assertDoesNotThrow(() -> printNode.process(msg));
    }
}
