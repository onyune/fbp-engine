package com.fbp.engine.parser;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record FlowDefinition(
        String id,
        String name,
        String description,
        List<NodeDefinition> nodes,
        List<ConnectionDefinition> connections
) {
    public FlowDefinition {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        connections = connections == null ? List.of() : List.copyOf(connections);

        Set<String> nodeIds = nodes.stream()
                .map(NodeDefinition::id)
                .collect(Collectors.toSet());

        for (ConnectionDefinition conn : connections) {
            if (!nodeIds.contains(conn.sourceId())) {
                throw new IllegalArgumentException("존재하지 않는 소스 노드를 참조하고 있습니다: " + conn.sourceId());
            }
            if (!nodeIds.contains(conn.targetId())) {
                throw new IllegalArgumentException("존재하지 않는 타겟 노드를 참조하고 있습니다: " + conn.targetId());
            }
        }
    }

    public NodeDefinition getNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
}