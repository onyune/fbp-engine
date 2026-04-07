//package com.fbp.engine.core.impl;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import com.fbp.engine.core.InputPort;
//import com.fbp.engine.message.Message;
//import com.fbp.engine.node.Node;
//import java.util.HashMap;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//class DefaultInputPortTest {
//
//    @Test
//    @DisplayName("receive 시 owner 호출 && 포트 이름 확인")
//    void testCallOwner(){
//        abstract class DummyNode implements Node {
//            boolean processed = false;
//            @Override public String getId() { return "dummy"; }
//            @Override public void process(Message message) { processed = true; }
//        }
//        DummyNode node = new DummyNode();
//        InputPort in = new DefaultInputPort("in", node);
//
//        assertEquals("in", in.getName());
//
//        in.receive(new Message(new HashMap<>()));
//        assertTrue(node.processed);
//    }
//
//}