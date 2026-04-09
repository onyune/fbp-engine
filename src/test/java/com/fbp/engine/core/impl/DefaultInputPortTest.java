package com.fbp.engine.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultInputPortTest {

    @Test
    @DisplayName("receive 시 owner 호출")
    void testCallOwner() {
        Node mockNode = Mockito.mock(Node.class);
        InputPort in = new DefaultInputPort("in", mockNode);
        Message msg = new Message(new HashMap<>());

        in.receive(msg);

        verify(mockNode, times(1)).process(Mockito.any(Message.class));
    }

    @Test
    @DisplayName("포트 이름 확인")
    void testGetName() {
        Node mockNode = Mockito.mock(Node.class);
        InputPort in = new DefaultInputPort("test-port", mockNode);

        assertEquals("test-port", in.getName());
    }
}