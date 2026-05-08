package com.fbp.engine.runner;

import com.fbp.engine.api.HttpApiServer;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.TimerNode;
import com.fbp.engine.registry.NodeRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EngineMain {

    public static void main(String[] args) {
        log.info("=== FBP 엔진 및 API 서버 부팅을 시작합니다 ===");

        try {
            NodeRegistry nodeRegistry = new NodeRegistry();

            nodeRegistry.register("GeneratorNode", config ->
                    new GeneratorNode(config.getOrDefault("id", "gen").toString()));

            nodeRegistry.register("LogNode", config ->
                    new LogNode(config.getOrDefault("id", "log").toString()));

            nodeRegistry.register("TimerNode", config -> {
                String id = config.getOrDefault("id", "timer").toString();
                long interval = Long.parseLong(config.getOrDefault("intervalMs", "1000").toString());
                return new TimerNode(id, interval);
            });

            nodeRegistry.register("FilterNode", config -> {
                String id = config.getOrDefault("id", "filter").toString();
                String key = config.getOrDefault("key", "value").toString();
                double threshold = Double.parseDouble(config.getOrDefault("threshold", "50.0").toString());
                return new FilterNode(id, key, threshold);
            });

            FlowEngine flowEngine = new FlowEngine();
            FlowManager flowManager = new FlowManager(nodeRegistry, flowEngine);

            int port = 8080;
            HttpApiServer apiServer = new HttpApiServer(port);
            apiServer.start(flowManager);

            log.info("=== [성공] HTTP API 서버가 포트 {} 에서 실행 중입니다 ===", port);
            log.info("💡 Postman이나 터미널에서 다음 API를 호출해 보세요:");
            log.info("   - 상태 확인: GET http://localhost:{}/health", port);
            log.info("   - 플로우 배포: POST http://localhost:{}/flows", port);

            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("서버 부팅 중 치명적인 에러 발생: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}