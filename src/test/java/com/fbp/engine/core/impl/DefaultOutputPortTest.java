package com.fbp.engine.core.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultOutputPortTest {

    @Test
    @DisplayName("단일 Connection 전달")
    void singleConnection(){
        OutputPort outputPort = new DefaultOutputPort("out");
        Connection conn1 = new Connection("conn-1");
        outputPort.connect(conn1);

        Map<String, Object> data = new HashMap<>();
        data.put("test", 1);

        outputPort.send(new Message(data));
        assertEquals(1, conn1.getBufferSize());
    }

    @Test
    @DisplayName("다중 Connection 전달")
    void multiConnections(){
        OutputPort outputPort = new DefaultOutputPort("out");
        Connection conn1 = new Connection("conn-1");
        Connection conn2 = new Connection("conn-2");
        outputPort.connect(conn1);
        outputPort.connect(conn2);

        Map<String, Object> data = new HashMap<>();
        data.put("test", 1);

        outputPort.send(new Message(data));
        assertEquals(1, conn1.getBufferSize());
        assertEquals(1, conn2.getBufferSize());
    }

    @Test
    @DisplayName("Connection 미연결 시")
    void notConnect(){
        OutputPort outputPort = new DefaultOutputPort("out");
        Map<String, Object> data = new HashMap<>();
        data.put("test", 1);

        assertDoesNotThrow(()->
                outputPort.send(new Message(data)));


    }

}