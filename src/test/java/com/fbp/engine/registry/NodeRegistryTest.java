package com.fbp.engine.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeRegistryTest {

    private NodeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
    }

    @Test
    @DisplayName("1. register + create")
    void testRegisterAndCreate() {
        NodeFactory factory = config -> new DummyNode("dummy-id");
        registry.register("MqttSubscriber", factory);

        Node node = registry.create("MqttSubscriber", new HashMap<>());

        assertNotNull(node);
        assertTrue(node instanceof DummyNode);
        assertEquals("dummy-id", node.getId());
    }

    @Test
    @DisplayName("2. 미등록 타입 create")
    void testCreateUnregisteredType() {
        assertThrows(NodeRegistryException.class, () -> {
            registry.create("UnknownType", new HashMap<>());
        });
    }

    @Test
    @DisplayName("3. 중복 등록 처리")
    void testDuplicateRegistration() {
        NodeFactory factory1 = config -> new DummyNode("node1");
        NodeFactory factory2 = config -> new DummyNode("node2");

        registry.register("DuplicateNode", factory1);

        assertThrows(NodeRegistryException.class, () -> {
            registry.register("DuplicateNode", factory2);
        });

        assertDoesNotThrow(() -> {
            registry.register("DuplicateNode", factory2, true);
        });

        Node node = registry.create("DuplicateNode", new HashMap<>());
        assertEquals("node2", node.getId());
    }

    @Test
    @DisplayName("4. getRegisteredTypes")
    void testGetRegisteredTypes() {
        registry.register("TypeA", config -> new DummyNode("A"));
        registry.register("TypeB", config -> new DummyNode("B"));

        Set<String> types = registry.getRegisteredTypes();

        assertEquals(2, types.size());
        assertTrue(types.contains("TypeA"));
        assertTrue(types.contains("TypeB"));
    }

    @Test
    @DisplayName("5. config 전달")
    void testConfigPassing() {
        NodeFactory factory = config -> {
            String id = (String) config.get("id");
            return new DummyNode(id);
        };
        registry.register("ConfigNode", factory);

        Map<String, Object> config = new HashMap<>();
        config.put("id", "custom-sensor-id");

        Node node = registry.create("ConfigNode", config);

        assertEquals("custom-sensor-id", node.getId(), "config로 전달된 ID가 설정되어야 합니다.");
    }

    @Test
    @DisplayName("6. isRegistered")
    void testIsRegistered() {
        registry.register("RegisteredType", config -> new DummyNode("1"));

        assertTrue(registry.isRegistered("RegisteredType"));
        assertFalse(registry.isRegistered("UnregisteredType"));
    }

    @Test
    @DisplayName("7. null/빈 타입명")
    void testNullOrEmptyTypeName() {
        NodeFactory factory = config -> new DummyNode("1");

        assertThrows(NodeRegistryException.class, () -> registry.register(null, factory));
        assertThrows(NodeRegistryException.class, () -> registry.register("", factory));
        assertThrows(NodeRegistryException.class, () -> registry.register("   ", factory));

        assertThrows(NodeRegistryException.class, () -> registry.create(null, new HashMap<>()));
        assertThrows(NodeRegistryException.class, () -> registry.create("  ", new HashMap<>()));

        assertThrows(NodeRegistryException.class, () -> registry.isRegistered(null));
        assertThrows(NodeRegistryException.class, () -> registry.isRegistered(""));
    }

    /**
     * 테스트용 더미 노드 (엔진의 기본 Node 인터페이스 구현)
     */
    static class DummyNode implements Node {
        private final String id;

        public DummyNode(String id) {
            this.id = id;
        }

        @Override
        public String getId() { return id; }

        @Override
        public void process(Message message) {

        }

        @Override
        public void initialize() {

        }

        @Override
        public void shutdown() {

        }
    }
}