package com.fbp.engine.api;

import com.fbp.engine.core.Flow;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.FlowMetrics;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetrics;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * /flows /flows/{id} GET : 실행 중인 플로우 목록 POST : 새 플로우 배포 DELETE : 플로우 중지 및 삭제
 */
@Slf4j
public class FlowHandler implements HttpHandler {
    private final FlowManager flowManager;
    private final JsonFlowParser parser = new JsonFlowParser();

    public FlowHandler(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                // GET /flows - 목록 조회
                if (path.endsWith("/metrics")) {
                    handleMetrics(exchange, path);
                }
                // GET /flows (목록 조회) 인 경우
                else {
                    List<Map<String, String>> flows = flowManager.list().stream()
                            .map(f -> Map.of(
                                    "id", f.getId(),
                                    "name", f.getName(),
                                    "status", f.getState().name()
                            ))
                            .toList();
                    ApiResponse.success(flows).send(exchange, 200);
                }
            } else if ("POST".equals(method)) {
                // POST /flows - 신규 배포
                FlowDefinition def = parser.parse(exchange.getRequestBody());
                flowManager.deploy(def);
                ApiResponse.success(Map.of("id", def.id(), "status", "DEPLOYED")).send(exchange, 201);
            } else if ("DELETE".equals(method)) {
                // DELETE /flows/{id} - 삭제
                String flowId = path.substring("/flows/".length());
                if (flowId.isEmpty()) {
                    throw new IllegalArgumentException("플로우 ID가 필요합니다.");
                }

                flowManager.remove(flowId);
                ApiResponse.success(Map.of("message", "Flow removed: " + flowId)).send(exchange, 200);
            } else {
                ApiResponse.error("Unsupported Method").send(exchange, 405);
            }
        } catch (Exception e) {
            log.error("[FlowHandler] 처리 중 예외 발생 (경로: {})", path, e);
            ApiResponse.error(e.getMessage()).send(exchange, 400);
        }
    }

    private void handleMetrics(HttpExchange exchange, String path) {
        String flowId = path.substring("/flows/".length(), path.lastIndexOf("/metrics"));
        Flow flow = flowManager.getFlow(flowId);

        if (flow == null) {
            ApiResponse.error("Flow not found").send(exchange, 404);
            return;
        }

        List<String> nodeIds = flow.getNodes().stream()
                .map(node -> node.getId())
                .toList();
        FlowMetrics flowMetrics = MetricsCollector.getInstance().getFlowMetrics(flowId, nodeIds);

        List<Map<String, Object>> nodeStats = flowMetrics.getNodes().entrySet().stream().map(entry -> {
            String nodeId = entry.getKey();
            NodeMetrics nm = entry.getValue();

            if (nm == null) {
                nm = new NodeMetrics();
            }

            return Map.<String, Object>of(
                    "id", nodeId,
                    "processed", nm.getProcessedCount(),
                    "errors", nm.getErrorCount(),
                    "avgTime", nm.getAverageTime()
            );
        }).toList();

        Map<String, Object> responseData = Map.of(
                "totalProcessed", flowMetrics.getTotalProcessed(),
                "totalErrors", flowMetrics.getTotalErrors(),
                "overallAvgTime", flowMetrics.getOverallAverageTime(),
                "nodes", nodeStats
        );
        ApiResponse.success(responseData).send(exchange, 200);
    }
}