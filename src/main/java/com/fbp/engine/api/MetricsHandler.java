package com.fbp.engine.api;

import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetrics;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;

/// /flows/{id}/metrics, /nodes/{id}/stats
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

        // GET /nodes/{id}/stats (노드 상세 통계)
        if (path.startsWith("/nodes/") && path.endsWith("/stats")) {
            String nodeId = path.substring("/nodes/".length(), path.indexOf("/stats"));
            NodeMetrics nm = MetricsCollector.getInstance().getMetrics(nodeId);

            if (nm == null) {
                ApiResponse.error("Node metrics not found").send(exchange, 404);
                return;
            }
            //TODO : step9에서 queue 만들때 집어넣기
            int currentQueueSize = 0;
            //  {processed, errors, avgTime, queueSize}
            Map<String, Object> stats = Map.of(
                    "processed", nm.getProcessedCount(),
                    "errors", nm.getErrorCount(),
                    "avgTime", nm.getAverageTime(),
                    "queueSize", currentQueueSize
            );

            ApiResponse.success(stats).send(exchange, 200);
        } else {
            ApiResponse.error("Invalid Metrics Path").send(exchange, 404);
        }
    }
}
