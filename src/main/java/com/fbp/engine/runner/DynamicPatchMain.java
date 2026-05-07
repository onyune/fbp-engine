package com.fbp.engine.runner;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.registry.NodeRegistry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicPatchMain {

    public static void main(String[] args) throws Exception {
        NodeRegistry nodeRegistry = new NodeRegistry();

        nodeRegistry.register("GeneratorNode", config ->
                new GeneratorNode(config.getOrDefault("id", "gen").toString()));

        nodeRegistry.register("LogNode", config ->
                new LogNode(config.getOrDefault("id", "log").toString()));

        nodeRegistry.register("FilterNode", config -> {
            String id = config.getOrDefault("id", "filter").toString();
            String key = config.getOrDefault("key", "value").toString();
            double threshold = Double.parseDouble(config.getOrDefault("threshold", "50.0").toString());
            return new FilterNode(id, key, threshold);
        });

        FlowEngine flowEngine = new FlowEngine();
        FlowManager flowManager = new FlowManager(nodeRegistry, flowEngine);
        JsonFlowParser parser = new JsonFlowParser();

        String initialJson = """
            {
              "id": "hot-patch-test",
              "name": "Version 1.0",
              "nodes": [
                {"id": "gen", "type": "GeneratorNode", "config": {"id": "gen"}},
                {"id": "log1", "type": "LogNode", "config": {"id": "log1"}}
              ],
              "connections": [
                {"from": "gen:out", "to": "log1:in"}
              ]
            }
            """;

        log.info("=== 1. 초기 플로우 배포 (Version 1.0) ===");
        FlowDefinition initialDef = parser.parse(new ByteArrayInputStream(initialJson.getBytes(StandardCharsets.UTF_8)));
        flowManager.deploy(initialDef);

        Thread.sleep(3000);

        String patchedJson = """
            {
              "id": "hot-patch-test",
              "name": "Version 2.0",
              "nodes": [
                {"id": "gen", "type": "GeneratorNode", "config": {"id": "gen"}},
                {"id": "filter", "type": "FilterNode", "config": {"id": "filter", "key": "tick", "threshold": 5.0}},
                {"id": "log1", "type": "LogNode", "config": {"id": "log1"}}
              ],
              "connections": [
                {"from": "gen:out", "to": "filter:in"},
                {"from": "filter:out", "to": "log1:in"}
              ]
            }
            """;

        log.info("=== 2. 플로우 동적 패치 실행 (Version 2.0) ===");
        FlowDefinition patchedDef = parser.parse(new ByteArrayInputStream(patchedJson.getBytes(StandardCharsets.UTF_8)));

        flowManager.patch("hot-patch-test", patchedDef);

        Thread.sleep(3000);

        log.info("=== 3. 테스트 종료 및 엔진 정지 ===");
        flowManager.stop("hot-patch-test");
        System.exit(0);
    }
}