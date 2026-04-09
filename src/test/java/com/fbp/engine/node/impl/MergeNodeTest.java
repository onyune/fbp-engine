package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MergeNodeTest {
    MergeNode mergeNode;
    Connection outConn;

    @BeforeEach
    void setUp(){
        mergeNode=new MergeNode("merge-1");
        outConn = new Connection("outConn");

        mergeNode.getOutputPort("out").connect(outConn);
    }

    @Test
    @DisplayName("양쪽 입력 수신 && 합쳐진 메시지 출력")
    void testMergeMessage(){
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("temperature", 25.5);
        Message msg1 = new Message(payload1);

        Map<String, Object> payload2 = new HashMap<>();
        payload2.put("humidity", 60.0);
        Message msg2 = new Message(payload2);

        mergeNode.getInputPort("in-1").receive(msg1);
        mergeNode.getInputPort("in-2").receive(msg2);

        assertEquals(1, outConn.getBufferSize());

        Message mergedMsg = outConn.poll();
        assertNotNull(mergedMsg);

        assertTrue(mergedMsg.hasKey("in1_temperature"));
        assertTrue(mergedMsg.hasKey("in2_humidity"));
        assertEquals(25.5, mergedMsg.get("in1_temperature"));
        assertEquals(60.0, mergedMsg.get("in2_humidity"));
        assertEquals("merge-1", mergedMsg.get("mergedBy"));
    }

    @Test
    @DisplayName("한쪽만 도착 시 대기")
    void testWaitWhenOnlyOneArrives(){
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("temperature", 25.5);
        Message msg1 = new Message(payload1);

        mergeNode.getInputPort("in-1").receive(msg1);
        assertEquals(0, outConn.getBufferSize());
    }

    @Test
    @DisplayName("포트 구성 확인")
    void testPortConfiguration(){
        assertNotNull(mergeNode.getInputPort("in-1"));
        assertNotNull(mergeNode.getInputPort("in-2"));
        assertNotNull(mergeNode.getOutputPort("out"));
    }

}