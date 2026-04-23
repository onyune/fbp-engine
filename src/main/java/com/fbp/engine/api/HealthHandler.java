package com.fbp.engine.api;

import com.fbp.engine.engine.FlowManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;

/**
 * /health
 * 엔진이 살아 있는지, 가동시간은 얼마나 되었는지 간단한 정보 반환
 */
public class HealthHandler implements HttpHandler {
    private final FlowManager flowManager;
    private final long startTime = System.currentTimeMillis();

    public HealthHandler(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(!"GET".equals(exchange.getRequestMethod())){
            ApiResponse.error("Method Not Allowed").send(exchange,405);
            return;
        }

        Map<String, Object> status = Map.of(
                "status","UP",
                "uptime", (System.currentTimeMillis() - startTime) / 1000 + "s",
                "flowCounter", flowManager.list().size()
        );

        ApiResponse.success(status).send(exchange,200);
    }
}
