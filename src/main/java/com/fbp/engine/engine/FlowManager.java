package com.fbp.engine.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.Flow.FlowState;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.core.impl.JsonMessageSerializer;
import com.fbp.engine.core.impl.LocalConnection;
import com.fbp.engine.core.impl.MqttBridgeConnection;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.Node;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.parser.TransportDefinition;
import com.fbp.engine.registry.NodeRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        TransportDefinition transportDef = definition.transport();
        // 3. 포트 연결 (Wire-up)
        for (ConnectionDefinition connDef : definition.connections()) {
            if (!createdNodes.containsKey(connDef.sourceId()) || !createdNodes.containsKey(connDef.targetId())) {
                throw new IllegalArgumentException("연결할 노드를 찾을 수 없습니다: " + connDef.sourceId() + " -> " + connDef.targetId());
            }
            String connId = String.format("%s:%s->%s:%s", connDef.sourceId(),connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
            Connection connection;

            if(transportDef != null && "mqtt".equalsIgnoreCase(transportDef.type())){
                String topic = String.format("fbp/%s/%s.%s->%s.%s",
                        definition.id(),
                        connDef.sourceId(), connDef.sourcePort(),
                        connDef.targetId(), connDef.targetPort());
                int qos = (transportDef.qos() != null ) ? transportDef.qos() : null ;
                connection = new MqttBridgeConnection(
                        connId,
                        transportDef.broker(),
                        topic,
                        new JsonMessageSerializer(),
                        qos
                );
                log.info("[FlowManager] MQTT 브릿지 연결 생성 - 토픽: {}", topic);
            }else{
                connection = new LocalConnection(connId);
                log.info("[FlowManager] 로컬 연결 생성 - {}", connId);
            }
            flow.connect(connection,connDef.sourceId(), connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
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

    /**
     * 동적 실행 중 노드 추가
     * @param flowId  추가할 플로우
     * @param nodeDef 노드 상세
     */
    public void addNode(String flowId, NodeDefinition nodeDef){
        Flow flow = getFlow(flowId);
        if (flow == null) throw new IllegalArgumentException("플로우를 찾을 수 없습니다: " + flowId);
        if(flow.getNodes().stream().anyMatch(n -> n.getId().equals(nodeDef.id()))){
            throw new IllegalArgumentException("이미 존재하는 노드 ID입니다: " + nodeDef.id());
        }

        Node node = nodeRegistry.create(nodeDef.type(),nodeDef.config());
        AbstractNode absNode = (AbstractNode) node;
        absNode.setFlowId(flowId);
        flow.addNode(absNode);

        //플로우가 실행 중이라면 방금 추가된 노드도 바로 실행
        if(flow.getState() == FlowState.RUNNING){
            absNode.initialize();
        }
    }

    /**
     * 동적 실행 중 커넥션 추가
     * @param flowId 추가할 플로우
     * @param connDef  커넥션 정보
     * @param transportDef  transport 유무에 따라 Connection 생성을 위함
     */
    public void addConnection(String flowId, ConnectionDefinition connDef, TransportDefinition transportDef){
        Flow flow = getFlow(flowId);
        if (flow == null) throw new IllegalArgumentException("플로우를 찾을 수 없습니다: " + flowId);

        String connId = String.format("%s:%s->%s:%s", connDef.sourceId(), connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
        Connection connection;

        if (transportDef != null && "mqtt".equalsIgnoreCase(transportDef.type())) {
            String topic = String.format("fbp/%s/%s.%s->%s.%s", flowId, connDef.sourceId(), connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
            int qos = (transportDef.qos() != null) ? transportDef.qos() : 1;
            connection = new MqttBridgeConnection(connId, transportDef.broker(), topic, new JsonMessageSerializer(), qos);
        } else {
            connection = new LocalConnection(connId);
        }
        flow.connect(connection, connDef.sourceId(), connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
    }

    /**
     * 런타임에 연결 끊기
     * @param flowId 플로우 아이디
     * @param connId 끊을 연결 아이디
     */
    public void removeConnection(String flowId, String connId){
        Flow flow = getFlow(flowId);
        if(flow!=null){
            flow.removeConnection(connId);
        }
    }

    /**
     * 런타임에 노드 삭제
     * @param flowId 플로우 아아디
     * @param nodeId 삭제할 노드 아아디
     */
    public void removeNode(String flowId, String nodeId){
        Flow flow = getFlow(flowId);
        if(flow==null) return;

        //선 정리
        flow.getConnections().stream()
                .filter(c->c.getId().startsWith(nodeId+":")|| c.getId().contains("->"+ nodeId+":"))
                .map(Connection::getId)
                .toList()
                .forEach(flow::removeConnection);

        flow.removeNode(nodeId);
    }

    public void patch(String flowId, FlowDefinition newDef) {
        Flow existingFlow = getFlow(flowId);
        if (existingFlow == null) {
            throw new IllegalArgumentException("존재하지 않는 플로우입니다: " + flowId);
        }

        Set<String> existingNodeIds = existingFlow.getNodes().stream()
                .map(Node::getId)
                .collect(Collectors.toSet());

        Set<String> newNodeIds = newDef.nodes().stream()
                .map(NodeDefinition::id)
                .collect(Collectors.toSet());

        Set<String> existingConnIds = existingFlow.getConnections().stream()
                .map(Connection::getId)
                .collect(Collectors.toSet());

        Set<String> newConnIds = newDef.connections().stream()
                .map(c -> String.format("%s:%s->%s:%s", c.sourceId(), c.sourcePort(), c.targetId(), c.targetPort()))
                .collect(Collectors.toSet());

        Set<String> nodesToRemove = new java.util.HashSet<>(existingNodeIds);
        nodesToRemove.removeAll(newNodeIds);

        Set<NodeDefinition> nodesToAdd = newDef.nodes().stream()
                .filter(n -> !existingNodeIds.contains(n.id()))
                .collect(Collectors.toSet());

        Set<String> connsToRemove = new java.util.HashSet<>(existingConnIds);
        connsToRemove.removeAll(newConnIds);

        Set<ConnectionDefinition> connsToAdd = newDef.connections().stream()
                .filter(c -> {
                    String id = String.format("%s:%s->%s:%s", c.sourceId(), c.sourcePort(), c.targetId(), c.targetPort());
                    return !existingConnIds.contains(id);
                })
                .collect(Collectors.toSet());

        log.info("[Patch Diff] -Nodes: {}, +Nodes: {}, -Wires: {}, +Wires: {}",
                nodesToRemove.size(), nodesToAdd.size(), connsToRemove.size(), connsToAdd.size());

        for (String connId : connsToRemove) {
            removeConnection(flowId, connId);
            log.info("  [-] Wire 제거 완료: {}", connId);
        }

        for (String nodeId : nodesToRemove) {
            removeNode(flowId, nodeId);
            log.info("  [-] Node 제거 완료: {}", nodeId);
        }

        for (NodeDefinition nodeDef : nodesToAdd) {
            addNode(flowId, nodeDef);
            log.info("  [+] Node 추가 완료: {}", nodeDef.id());
        }

        TransportDefinition transportDef = newDef.transport();
        for (ConnectionDefinition connDef : connsToAdd) {
            addConnection(flowId, connDef, transportDef);
            String connId = String.format("%s:%s->%s:%s", connDef.sourceId(), connDef.sourcePort(), connDef.targetId(), connDef.targetPort());
            log.info("  [+] Wire 추가 완료: {}", connId);
        }

        log.info("[FlowManager] 플로우 [{}] 동적 패치 완료!", flowId);
    }
}