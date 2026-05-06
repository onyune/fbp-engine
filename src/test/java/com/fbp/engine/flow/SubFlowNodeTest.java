package com.fbp.engine.flow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubFlowNodeTest {

    static class DummyNode extends AbstractNode {
        public DummyNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }
        @Override
        protected void onProcess(Message message) {
            send("out", message);
        }
    }

    @Test
    @DisplayName("메시지 전달")
    void testMessageDelivery() {
        Flow innerFlow = new Flow("inner");
        DummyNode entryNode = new DummyNode("entry");
        innerFlow.addNode(entryNode);

        SubFlowNode subFlow = new SubFlowNode("sub", innerFlow, "entry");
        innerFlow.connect("entry", "out", subFlow.getBridgeNodeId(), "in");

        Connection mockConn = mock(Connection.class);
        subFlow.getOutputPort("out").connect(mockConn);

        subFlow.initialize();
        subFlow.process(new Message(new HashMap<>()));

        verify(mockConn, timeout(1000).times(1)).deliver(any(Message.class));
        subFlow.shutdown();
    }

    @Test
    @DisplayName("내부 플로우 실행")
    void testInternalFlowExecution() {
        Flow innerFlow = new Flow("inner");
        DummyNode entryNode = new DummyNode("entry");
        DummyNode nextNode = new DummyNode("next");
        innerFlow.addNode(entryNode).addNode(nextNode);

        innerFlow.connect("entry", "out", "next", "in");

        SubFlowNode subFlow = new SubFlowNode("sub", innerFlow, "entry");
        innerFlow.connect("next", "out", subFlow.getBridgeNodeId(), "in");

        Connection mockConn = mock(Connection.class);
        subFlow.getOutputPort("out").connect(mockConn);

        subFlow.initialize();
        subFlow.process(new Message(new HashMap<>()));

        verify(mockConn, timeout(1000).times(1)).deliver(any(Message.class));
        subFlow.shutdown();
    }

    @Test
    @DisplayName("수명주기 - 시작")
    void testLifecycleStart() {
        Flow innerFlow = new Flow("inner");
        innerFlow.addNode(new DummyNode("entry"));
        SubFlowNode subFlow = new SubFlowNode("sub", innerFlow, "entry");

        subFlow.initialize();
        assertEquals(Flow.FlowState.RUNNING, innerFlow.getState());
        subFlow.shutdown();
    }

    @Test
    @DisplayName("수명주기 - 정지")
    void testLifecycleStop() {
        Flow innerFlow = new Flow("inner");
        innerFlow.addNode(new DummyNode("entry"));
        SubFlowNode subFlow = new SubFlowNode("sub", innerFlow, "entry");

        subFlow.initialize();
        subFlow.shutdown();
        assertEquals(Flow.FlowState.STOPPED, innerFlow.getState());
    }

    @Test
    @DisplayName("재사용")
    void testReusability() {
        Flow inner1 = new Flow("inner1");
        inner1.addNode(new DummyNode("entry1"));
        SubFlowNode sub1 = new SubFlowNode("sub1", inner1, "entry1");

        Flow inner2 = new Flow("inner2");
        inner2.addNode(new DummyNode("entry2"));
        SubFlowNode sub2 = new SubFlowNode("sub2", inner2, "entry2");

        assertNotEquals(sub1.getId(), sub2.getId());
        assertNotEquals(sub1.getBridgeNodeId(), sub2.getBridgeNodeId());
        assertNotEquals(sub1.getBridgeErrorNodeId(), sub2.getBridgeErrorNodeId());
    }

    @Test
    @DisplayName("내부 에러 전파")
    void testInternalErrorPropagation() {
        AbstractNode errorNode = new AbstractNode("error-node") {
            @Override
            protected void onProcess(Message message) {
                throw new RuntimeException("Internal Error");
            }
        };
        Flow errorFlow = new Flow("error-flow");
        errorFlow.addNode(errorNode);

        SubFlowNode errorSubFlow = new SubFlowNode("subflow-error", errorFlow, "error-node");

        Connection mockErrorConn = mock(Connection.class);
        errorSubFlow.getOutputPort("error").connect(mockErrorConn);

        errorSubFlow.initialize();
        assertDoesNotThrow(() -> errorSubFlow.process(new Message(new HashMap<>())));

        verify(mockErrorConn, timeout(1000).times(1)).deliver(any(Message.class));
        errorSubFlow.shutdown();
    }

    @Test
    @DisplayName("JSON 정의")
    void testJsonDefinition() {
        Flow innerFlow = new Flow("inner");
        innerFlow.addNode(new DummyNode("entry"));
        SubFlowNode subFlow = new SubFlowNode("sub", innerFlow, "entry");

        assertEquals("sub-bridge-out", subFlow.getBridgeNodeId());
        assertEquals("sub-bridge-error", subFlow.getBridgeErrorNodeId());

        assertNotNull(innerFlow.getNodes().stream()
                .filter(n -> n.getId().equals("sub-bridge-out"))
                .findFirst().orElse(null));

        assertNotNull(innerFlow.getNodes().stream()
                .filter(n -> n.getId().equals("sub-bridge-error"))
                .findFirst().orElse(null));
    }
}