//package com.fbp.engine.core;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//import com.fbp.engine.message.Message;
//import java.util.HashMap;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.InOrder;
//import org.mockito.Mockito;
//
//public class QueueConnectionTest {
//    @Test
//    @DisplayName("deliver 후 target 수신")
//    void testDeliverToTarget() {
//        Connection conn = new Connection("conn-1");
//        InputPort mockTarget = Mockito.mock(InputPort.class);
//        conn.setTarget(mockTarget);
//
//        Message msg = new Message(new HashMap<>());
//        conn.deliver(msg);
//
//        verify(mockTarget, times(1)).receive(msg);
//    }
//
//    @Test
//    @DisplayName("target 미설정 시 동작")
//    void testDeliverWithoutTarget() {
//        Connection conn = new Connection("conn-1");
//        Message msg = new Message(new HashMap<>());
//
//        assertDoesNotThrow(() -> conn.deliver(msg));
//    }
//
//    @Test
//    @DisplayName("버퍼 크기 확인")
//    void testBufferSize() {
//        Connection conn = new Connection("conn-1");
//
//        Message msg1 = new Message(new HashMap<>());
//        Message msg2 = new Message(new HashMap<>());
//
//        conn.deliver(msg1);
//        conn.deliver(msg2);
//
//        assertEquals(2, conn.getBufferSize());
//    }
//
//    @Test
//    @DisplayName("다수 메시지 순서 보장")
//    void testMessageOrder() {
//        Connection conn = new Connection("conn-1");
//        InputPort mockTarget = Mockito.mock(InputPort.class);
//        conn.setTarget(mockTarget);
//
//        Message msg1 = new Message(new HashMap<>());
//        Message msg2 = new Message(new HashMap<>());
//        Message msg3 = new Message(new HashMap<>());
//
//        conn.deliver(msg1);
//        conn.deliver(msg2);
//        conn.deliver(msg3);
//
//        InOrder inOrder = Mockito.inOrder(mockTarget);
//        inOrder.verify(mockTarget).receive(msg1);
//        inOrder.verify(mockTarget).receive(msg2);
//        inOrder.verify(mockTarget).receive(msg3);
//    }
//}
