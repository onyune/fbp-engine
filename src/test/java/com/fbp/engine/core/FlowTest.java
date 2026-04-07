package com.fbp.engine.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlowTest {

    static class DummyNode extends AbstractNode {
        public DummyNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        protected void onProcess(Message message) {
        }
    }

    @Spy
    DummyNode n1 = new DummyNode("n1");

    @Spy
    DummyNode n2 = new DummyNode("n2");

    @Spy
    DummyNode n3 = new DummyNode("n3");

    @Test
    void test1_addNode() {
        Flow flow = new Flow("test-flow");
        flow.addNode(n1);

        List<AbstractNode> nodes = flow.getNodes();
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(n1));
    }

    @Test
    void test2_methodChaining() {
        Flow flow = new Flow("test-flow");

        assertDoesNotThrow(() -> {
            flow.addNode(n1).addNode(n2).connect("n1", "out", "n2", "in");
        });
    }

    @Test
    void test3_normalConnection() {
        Flow flow = new Flow("test-flow").addNode(n1).addNode(n2);

        assertEquals(0, flow.getConnections().size());
        flow.connect("n1", "out", "n2", "in");
        assertEquals(1, flow.getConnections().size());
    }

    @Test
    void test4_invalidSourceNodeId() {
        Flow flow = new Flow("test-flow").addNode(n2);

        assertThrows(IllegalArgumentException.class, () -> {
            flow.connect("wrongSource", "out", "n2", "in");
        });
    }

    @Test
    void test5_invalidTargetNodeId() {
        Flow flow = new Flow("test-flow").addNode(n1);

        assertThrows(IllegalArgumentException.class, () -> {
            flow.connect("n1", "out", "wrongTarget", "in");
        });
    }

    @Test
    void test6_invalidSourcePort() {
        Flow flow = new Flow("test-flow").addNode(n1).addNode(n2);

        assertThrows(IllegalArgumentException.class, () -> {
            flow.connect("n1", "wrongPort", "n2", "in");
        });
    }

    @Test
    void test7_invalidTargetPort() {
        Flow flow = new Flow("test-flow").addNode(n1).addNode(n2);

        assertThrows(IllegalArgumentException.class, () -> {
            flow.connect("n1", "out", "n2", "wrongPort");
        });
    }

    @Test
    void test8_validateEmptyFlow() {
        Flow flow = new Flow("empty");
        List<String> errors = flow.validate();

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("하나도 없습니다"));
    }

    @Test
    void test9_validateNormalFlow() {
        Flow flow = new Flow("test-flow")
                .addNode(n1).addNode(n2).connect("n1", "out", "n2", "in");

        assertTrue(flow.validate().isEmpty());
    }

    @Test
    void test10_initializeAll() {
        Flow flow = new Flow("test-flow").addNode(n1).addNode(n2);

        flow.initialize();

        verify(n1, times(1)).initialize();
        verify(n2, times(1)).initialize();
    }

    @Test
    void test11_shutdownAll() {
        Flow flow = new Flow("test-flow").addNode(n1).addNode(n2);

        flow.shutdown();

        verify(n1, times(1)).shutdown();
        verify(n2, times(1)).shutdown();
    }

    @Test
    void test12_cycleDetection() {
        Flow flow = new Flow("cycle-flow")
                .addNode(n1).addNode(n2).addNode(n3)
                .connect("n1", "out", "n2", "in")
                .connect("n2", "out", "n3", "in")
                .connect("n3", "out", "n1", "in");

        List<String> errors = flow.validate();

        assertFalse(errors.isEmpty(), "순환 참조 에러가 감지되어야 합니다.");
        assertTrue(errors.stream().anyMatch(err -> err.contains("순환 참조")),
                "에러 메시지에 '순환 참조'라는 단어가 포함되어야 합니다.");
    }


}