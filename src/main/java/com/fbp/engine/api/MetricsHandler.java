package com.fbp.engine.api;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetrics;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * /nodes/{id}/stats
 */
@Slf4j
public class MetricsHandler implements HttpHandler {
    private final FlowManager flowManager;

    public MetricsHandler(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            ApiResponse.error("Method Not Allowed").send(exchange, 405);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.startsWith("/nodes/") && path.endsWith("/stats")) {
                String nodeId = path.substring("/nodes/".length(), path.indexOf("/stats"));

                // 해당 노드가 어느 플로우에 속해있는지 찾음.
                String foundFlowId = null;
                Flow targetFlow =null;
                for (Flow flow : flowManager.list()) {
                    if (flow.getNodes().stream().anyMatch(n -> n.getId().equals(nodeId))) {
                        foundFlowId = flow.getId();
                        targetFlow=flow;
                        break;
                    }
                }

                if (foundFlowId == null) {
                    ApiResponse.error("Node not found in any active flow").send(exchange, 404);
                    return;
                }

                NodeMetrics nm = MetricsCollector.getInstance().getMetrics(foundFlowId, nodeId);

                int currentQueueSize = 0;
                for (Connection conn : targetFlow.getConnections()) {
                    String[] parts = conn.getId().split("->");
                    if (parts.length == 2) {
                        String targetPart = parts[1]; //"log1:in"
                        if (targetPart.startsWith(nodeId + ":")) {
                            currentQueueSize += conn.getBufferSize();
                        }
                    }
                }

                Map<String, Object> stats = Map.of(
                        "flowId", foundFlowId,
                        "nodeId", nodeId,
                        "processed", nm.getProcessedCount(),
                        "errors", nm.getErrorCount(),
                        "avgTime", String.format("%.2f ms", nm.getAverageTime()),
                        "queueSize", currentQueueSize
                );

                ApiResponse.success(stats).send(exchange, 200);
            } else {
                ApiResponse.error("Invalid Metrics Path").send(exchange, 404);
            }
        } catch (Exception e) {
            log.error("[MetricsHandler] 에러 발생", e);
            ApiResponse.error("Internal Server Error").send(exchange, 500);
        }
    }
}