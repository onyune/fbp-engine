package com.fbp.engine.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeFactoryTest {

    @Test
    @DisplayName("1. 정상 생성")
    void testNormalCreation() {
        // 검증 내용: config를 받아 올바른 노드 인스턴스 반환
        NodeFactory factory = config -> {
            String id = (String) config.getOrDefault("id", "default-id");
            return new DummyNode(id);
        };

        Map<String, Object> config = new HashMap<>();
        config.put("id", "my-sensor-node");

        Node node = factory.create(config);

        assertNotNull(node);
        assertEquals("my-sensor-node", node.getId(), "config에 전달된 값이 노드 생성에 반영되어야 합니다.");
    }

    @Test
    @DisplayName("2. 잘못된 config")
    void testInvalidConfigThrowsException() {
        // 검증 내용: 필수 설정이 누락된 config 전달 시 예외
        NodeFactory factory = config -> {
            if (!config.containsKey("brokerUrl")) {
                throw new IllegalArgumentException("필수 설정 'brokerUrl'이 누락되었습니다.");
            }
            return new DummyNode("mqtt-node");
        };

        Map<String, Object> invalidConfig = new HashMap<>(); // 필수 키 'brokerUrl' 누락

        assertThrows(IllegalArgumentException.class, () -> {
            factory.create(invalidConfig);
        });
    }

    @Test
    @DisplayName("3. 람다 구현")
    void testLambdaImplementation() {
        NodeFactory factory = config -> new DummyNode("lambda-node");

        Node node = factory.create(new HashMap<>());

        assertNotNull(node);
        assertEquals("lambda-node", node.getId(), "람다식으로 생성된 팩토리가 정상적으로 노드를 반환해야 합니다.");
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