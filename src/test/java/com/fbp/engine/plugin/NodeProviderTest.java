package com.fbp.engine.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import com.fbp.engine.registry.NodeFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeProviderTest {

    @Test
    @DisplayName("1. getNodeDescriptors: 구현체가 올바른 NodeDescriptor 목록 반환")
    void testGetNodeDescriptorsReturnsValidList() {
        NodeProvider provider = new NodeProvider() {
            @Override
            public List<NodeDescriptor> getNodeDescriptors() {
                return List.of(
                        new NodeDescriptor("DummyType", "테스트용 더미 노드", DummyNode.class, config -> new DummyNode())
                );
            }
        };

        List<NodeDescriptor> descriptors = provider.getNodeDescriptors();

        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        assertEquals("DummyType", descriptors.get(0).typeName());
    }

    @Test
    @DisplayName("2. 빈 목록: 노드를 제공하지 않는 Provider → 빈 리스트 반환")
    void testEmptyProviderReturnsEmptyList() {
        NodeProvider provider = () -> List.of();

        List<NodeDescriptor> descriptors = provider.getNodeDescriptors();

        assertNotNull(descriptors);
        assertTrue(descriptors.isEmpty());
    }

    @Test
    @DisplayName("3. descriptor 정합성: 반환된 descriptor의 typeName, factory가 null이 아님")
    void testDescriptorIntegrity() {
        NodeFactory factory = config -> new DummyNode();
        NodeProvider provider = () -> List.of(
                new NodeDescriptor("ValidType", "정상적인 노드", DummyNode.class, factory)
        );

        List<NodeDescriptor> descriptors = provider.getNodeDescriptors();
        NodeDescriptor descriptor = descriptors.get(0);

        assertNotNull(descriptor.typeName());
        assertNotNull(descriptor.factory());
    }

    static class DummyNode implements Node {
        @Override public String getId() { return "dummy"; }
        @Override public void process(Message message) {}
        @Override public void initialize() {}
        @Override public void shutdown() {}
    }
}