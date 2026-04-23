package com.fbp.engine.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.registry.NodeRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlowManagerTest {

    private FlowManager flowManager;
    private NodeRegistry registry;
    private FlowEngine flowEngine;
    private NodeDefinition dummyNodeDef;

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
        flowEngine = new FlowEngine();
        dummyNodeDef = new NodeDefinition("node1", "DummyType", Map.of("id", "node1"));

        registry.register("DummyType", config -> {
            String id = (String) config.getOrDefault("id", "default-id");
            return new DummyNode(id);
        });

        flowManager = new FlowManager(registry, flowEngine);
    }

    @Test
    @DisplayName("1. deploy: FlowDefinition으로 플로우 배포 → 실행 상태 확인")
    void testDeployAndCheckStatus() {
        FlowDefinition def = new FlowDefinition("flow-1", "", "", List.of(dummyNodeDef), List.of());
        flowManager.deploy(def);

        assertNotNull(flowManager.getStatus("flow-1"));
        assertTrue(flowEngine.getFlows().containsKey("flow-1"));
    }

    @Test
    @DisplayName("2. list: 배포된 플로우 목록 조회")
    void testListFlows() {
        FlowDefinition def1 = new FlowDefinition("flow-2a", "", "", List.of(dummyNodeDef), List.of());
        FlowDefinition def2 = new FlowDefinition("flow-2b", "", "", List.of(dummyNodeDef), List.of());

        flowManager.deploy(def1);
        flowManager.deploy(def2);

        assertEquals(2, flowManager.list().size());
    }

    @Test
    @DisplayName("3. getStatus: 특정 플로우의 상태(RUNNING, STOPPED) 조회")
    void testGetStatus() {
        FlowDefinition def = new FlowDefinition("flow-3", "", "", List.of(dummyNodeDef), List.of());
        flowManager.deploy(def);

        Flow.FlowState state = flowManager.getStatus("flow-3");
        assertNotNull(state);
    }

    @Test
    @DisplayName("4. stop: 실행 중인 플로우 정지")
    void testStopFlow() {
        FlowDefinition def = new FlowDefinition("flow-4", "", "", List.of(dummyNodeDef), List.of());
        flowManager.deploy(def);

        assertDoesNotThrow(() -> flowManager.stop("flow-4"));
    }

    @Test
    @DisplayName("5. restart: 정지된 플로우 재시작")
    void testRestartFlow() {
        FlowDefinition def = new FlowDefinition("flow-5", "", "", List.of(dummyNodeDef), List.of());
        flowManager.deploy(def);
        flowManager.stop("flow-5");

        assertDoesNotThrow(() -> flowManager.restart("flow-5"));
    }

    @Test
    @DisplayName("6. remove: 플로우 삭제 — 정지 후 제거")
    void testRemoveFlow() {
        FlowDefinition def = new FlowDefinition("flow-6", "", "", List.of(dummyNodeDef), List.of());
        flowManager.deploy(def);

        flowManager.remove("flow-6");
        assertEquals(0, flowManager.list().size());
    }

    @Test
    @DisplayName("7. 실행 중 삭제: RUNNING 상태의 플로우 삭제 시 자동 정지 후 삭제")
    void testRemoveRunningFlow() {
        FlowDefinition def = new FlowDefinition("flow-7", "", "", List.of(dummyNodeDef), List.of());
        flowManager.deploy(def);

        assertDoesNotThrow(() -> flowManager.remove("flow-7"));
        assertEquals(0, flowEngine.getFlows().size());
    }

    @Test
    @DisplayName("8. 존재하지 않는 id 조작: 없는 id로 stop/restart/remove 시 예외")
    void testNonExistentIdOperation() {
        assertThrows(IllegalArgumentException.class, () -> flowManager.stop("ghost"));
        assertThrows(IllegalArgumentException.class, () -> flowManager.restart("ghost"));
        assertThrows(IllegalArgumentException.class, () -> flowManager.remove("ghost"));
        assertThrows(IllegalArgumentException.class, () -> flowManager.getStatus("ghost"));
    }

    @Test
    @DisplayName("9. 중복 id 배포: 이미 존재하는 id의 플로우 배포 시 정책에 맞게 동작")
    void testDuplicateIdDeploy() {
        FlowDefinition def1 = new FlowDefinition("flow-dup", "", "", List.of(dummyNodeDef), List.of());
        FlowDefinition def2 = new FlowDefinition("flow-dup", "", "", List.of(dummyNodeDef), List.of());

        flowManager.deploy(def1);
        assertThrows(IllegalArgumentException.class, () -> flowManager.deploy(def2));
    }

    @Test
    @DisplayName("10. 미등록 노드 타입: FlowDefinition에 NodeRegistry에 없는 타입이 있으면 배포 실패")
    void testUnregisteredNodeTypeDeploy() {
        NodeDefinition unknownNode = new NodeDefinition("n1", "GhostType", Map.of());
        FlowDefinition def = new FlowDefinition("flow-fail", "", "", List.of(unknownNode), List.of());

        assertThrows(RuntimeException.class, () -> flowManager.deploy(def));
    }

    static class DummyNode extends AbstractNode {
        public DummyNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        public void shutdown() {}

        @Override
        protected void onProcess(Message message) {}
    }
}