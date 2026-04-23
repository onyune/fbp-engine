package com.fbp.engine.engine;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.Node;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.registry.NodeRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FlowManager {
    private final NodeRegistry nodeRegistry;
    private final FlowEngine flowEngine;

    public FlowManager(NodeRegistry nodeRegistry, FlowEngine flowEngine) {
        this.nodeRegistry = nodeRegistry;
        this.flowEngine = flowEngine;
    }

    /**
     * FlowDefinition을 바탕으로 플로우를 조립하고 FlowEngine에 배포 및 실행합니다.
     */
    public void deploy(FlowDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("FlowDefinition은 null일 수 없습니다.");
        }
        if (flowEngine.getFlows().containsKey(definition.id())) {
            throw new IllegalArgumentException("이미 존재하는 플로우 ID입니다: " + definition.id());
        }

        Flow flow = new Flow(definition.id());
        flow.setName(definition.name());
        Map<String, AbstractNode> createdNodes = new HashMap<>();

        for (NodeDefinition nodeDef : definition.nodes()) {
            Node node = nodeRegistry.create(nodeDef.type(), nodeDef.config());

            if (!(node instanceof AbstractNode)) {
                throw new RuntimeException("노드는 AbstractNode를 상속해야 합니다. (타입: " + nodeDef.type() + ")");
            }

            AbstractNode absNode = (AbstractNode) node;
            absNode.setFlowId(flow.getId());
            flow.addNode(absNode);
            createdNodes.put(nodeDef.id(), absNode);
        }

        // 3. 포트 연결 (Wire-up)
        for (ConnectionDefinition connDef : definition.connections()) {
            if (!createdNodes.containsKey(connDef.sourceId()) || !createdNodes.containsKey(connDef.targetId())) {
                throw new IllegalArgumentException("연결할 노드를 찾을 수 없습니다: " + connDef.sourceId() + " -> " + connDef.targetId());
            }
            flow.connect(connDef.sourceId(), connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
        }

        // 4. FlowEngine에 위임 (등록 및 실행)
        flowEngine.register(flow);
        flowEngine.startFlow(flow.getId());
    }

    /**
     * 실행 중인 플로우를 정지합니다.
     */
    public void stop(String flowId) {
        if (!flowEngine.getFlows().containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }
        flowEngine.stopFlow(flowId);
    }
    public Flow.FlowState getStatus(String flowId) {
        if (!flowEngine.getFlows().containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }
        return flowEngine.getFlows().get(flowId).getState();
    }

    /**
     * 특정 ID의 플로우 객체를 반환합니다. (MetricsHandler에서 사용)
     */
    public Flow getFlow(String flowId) {
        return flowEngine.getFlows().get(flowId);
    }

    public void restart(String flowId) {
        if (!flowEngine.getFlows().containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }
        flowEngine.startFlow(flowId);
    }

    /**
     * 플로우를 정지시키고 엔진에서 제거합니다.
     */
    public void remove(String flowId) {
        if (!flowEngine.getFlows().containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }
        flowEngine.stopFlow(flowId);
        flowEngine.getFlows().remove(flowId);
    }

    /**
     * 현재 엔진에 등록된 플로우 목록을 반환합니다.
     */
    public Collection<Flow> list() {
        return Collections.unmodifiableCollection(flowEngine.getFlows().values());
    }
}