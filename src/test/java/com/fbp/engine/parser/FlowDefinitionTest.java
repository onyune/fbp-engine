package com.fbp.engine.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlowDefinitionTest {

    @Test
    @DisplayName("1. 생성 후 노드/연결 목록 수정 불가 (불변성)")
    void testImmutability() {
        NodeDefinition n1 = new NodeDefinition("n1", "TypeA", Map.of());
        NodeDefinition n2 = new NodeDefinition("n2", "TypeB", Map.of());
        ConnectionDefinition conn = new ConnectionDefinition("n1", "out", "n2", "in");

        FlowDefinition def = new FlowDefinition("flow-1", "Test", "Desc", List.of(n1, n2), List.of(conn));

        assertThrows(UnsupportedOperationException.class, () -> def.nodes().add(new NodeDefinition("n3", "TypeC", Map.of())));
        assertThrows(UnsupportedOperationException.class, () -> def.connections().clear());
    }

    @Test
    @DisplayName("2. getNode(id)로 특정 노드 정의 조회")
    void testGetNode() {
        NodeDefinition n1 = new NodeDefinition("n1", "TypeA", Map.of());
        NodeDefinition n2 = new NodeDefinition("n2", "TypeB", Map.of());

        FlowDefinition def = new FlowDefinition("flow-2", "Test", "Desc", List.of(n1, n2), List.of());

        NodeDefinition found = def.getNode("n2");
        assertNotNull(found);
        assertEquals("TypeB", found.type());

        assertNull(def.getNode("unknown"));
    }

    @Test
    @DisplayName("3. 연결 유효성: 소스 노드가 존재하지 않을 때 예외 발생")
    void testInvalidSourceConnection() {
        NodeDefinition n2 = new NodeDefinition("n2", "TypeB", Map.of());
        ConnectionDefinition conn = new ConnectionDefinition("ghost", "out", "n2", "in");

        assertThrows(IllegalArgumentException.class, () -> {
            new FlowDefinition("flow-3", "Test", "Desc", List.of(n2), List.of(conn));
        });
    }

    @Test
    @DisplayName("3. 연결 유효성: 타겟 노드가 존재하지 않을 때 예외 발생")
    void testInvalidTargetConnection() {
        NodeDefinition n1 = new NodeDefinition("n1", "TypeA", Map.of());
        ConnectionDefinition conn = new ConnectionDefinition("n1", "out", "ghost", "in");

        assertThrows(IllegalArgumentException.class, () -> {
            new FlowDefinition("flow-4", "Test", "Desc", List.of(n1), List.of(conn));
        });
    }

    @Test
    @DisplayName("4. 원본 리스트 객체 변경으로부터 안전함 보장")
    void testOriginalListModification() {
        List<NodeDefinition> mutableNodes = new ArrayList<>();
        mutableNodes.add(new NodeDefinition("n1", "TypeA", Map.of()));

        FlowDefinition def = new FlowDefinition("flow-mut", "", "", mutableNodes, List.of());

        mutableNodes.add(new NodeDefinition("n2", "TypeB", Map.of()));
        assertEquals(1, def.nodes().size());
    }
}