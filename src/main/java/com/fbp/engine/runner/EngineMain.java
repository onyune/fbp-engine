package com.fbp.engine.runner;

import com.fbp.engine.api.HttpApiServer;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.engine.cli.FbpCli;
import com.fbp.engine.metrics.DomainMetricsExtractor;
import com.fbp.engine.metrics.DomainMetricsExtractor.DomainMetricRule;
import com.fbp.engine.node.impl.AlertNode;
import com.fbp.engine.node.impl.DynamicRouterNode;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.HealthCheckerNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.ModbusWriterNode;
import com.fbp.engine.node.impl.MqttPublisherNode;
import com.fbp.engine.node.impl.MqttSubscriberNode;
import com.fbp.engine.node.impl.ThresholdFilterNode;
import com.fbp.engine.node.impl.TimerNode;
import com.fbp.engine.registry.NodeRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EngineMain {

    public static void main(String[] args) {
        DomainMetricsExtractor.getInstance().addRule(
                "api-test-flow",
                new DomainMetricRule("temperature", "gen", "out", "value")
        );
        try {
            NodeRegistry nodeRegistry = new NodeRegistry();

            // 1. GeneratorNode 등록
            nodeRegistry.register("GeneratorNode", config -> {
                String id = config.getOrDefault("id", "gen-default").toString();
                return new GeneratorNode(id);
            });

            // 2. LogNode 등록
            nodeRegistry.register("LogNode", config -> {
                String id = config.getOrDefault("id", "log-default").toString();
                return new LogNode(id);
            });

            // 3. TimerNode 등록
            nodeRegistry.register("TimerNode", config -> {
                String id = config.getOrDefault("id", "timer").toString();
                return new TimerNode(id, Long.parseLong(config.getOrDefault("intervalMs", "1000").toString()));
            });

            // 4. FilterNode 등록
            nodeRegistry.register("FilterNode", config -> {
                String id = config.getOrDefault("id", "filter").toString();
                String key = config.getOrDefault("key", "value").toString();
                double threshold = Double.parseDouble(config.getOrDefault("threshold", "50.0").toString());
                return new FilterNode(id, key, threshold);
            });

            // 5. MqttSubscriberNode 등록
            nodeRegistry.register("MqttSubscriberNode", config -> {
                String id = config.getOrDefault("id", "mqtt-sub").toString();
                return new MqttSubscriberNode(id, config);
            });

            // 6. MqttPublisherNode 등록
            nodeRegistry.register("MqttPublisherNode", config -> {
                String id = config.getOrDefault("id", "mqtt-pub").toString();
                return new MqttPublisherNode(id, config);
            });

            // 7. DynamicRouterNode 등록
            nodeRegistry.register("DynamicRouterNode", config -> {
                String id = config.getOrDefault("id", "router").toString();
                return new DynamicRouterNode(id, config);
            });

            // 8. ThresholdFilterNode 등록 (Rule 역할)
            nodeRegistry.register("ThresholdFilterNode", config -> {
                String id = config.getOrDefault("id", "threshold-filter").toString();
                String field = (String) config.getOrDefault("field", "value");
                double threshold = Double.parseDouble(config.getOrDefault("threshold", "0.0").toString());
                return new ThresholdFilterNode(id, field, threshold);
            });

            // 9. ModbusWriterNode 등록
            nodeRegistry.register("ModbusWriterNode", config -> {
                String id = config.getOrDefault("id", "modbus-writer").toString();
                return new ModbusWriterNode(id, config);
            });

            // 10. AlertNode 등록
            nodeRegistry.register("AlertNode", config -> {
                String id = config.getOrDefault("id", "alert").toString();
                return new AlertNode(id);
            });

            // 11. HealthCheckerNode 등록
            nodeRegistry.register("HealthCheckerNode", config -> {
                String id = config.getOrDefault("id", "health-checker").toString();
                return new HealthCheckerNode(id);
            });

            FlowEngine flowEngine = new FlowEngine();
            FlowManager flowManager = new FlowManager(nodeRegistry, flowEngine);

            int port = 8080;
            HttpApiServer apiServer = new HttpApiServer(port);
            apiServer.start(flowManager);

            log.info("=== [성공] HTTP API 서버가 포트 {} 에서 실행 중입니다 ===", port);
            log.info("   - 상태 확인: GET http://localhost:{}/health", port);
            log.info("   - 플로우 배포: POST http://localhost:{}/flows", port);

            FbpCli cli = new FbpCli(flowManager);
            cli.start();
        } catch (Exception e) {
            log.error("서버 부팅 중 치명적인 에러 발생: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}