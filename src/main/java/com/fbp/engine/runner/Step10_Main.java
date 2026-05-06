package com.fbp.engine.runner;

import com.fbp.engine.api.HttpApiServer;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.plugin.PluginManager;
import com.fbp.engine.registry.NodeRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Step10_Main {

    public static void main(String[] args) {
        log.info("===============================================");
        log.info("FBP IoT Rule Engine 서버 부팅을 시작합니다...");
        log.info("===============================================");

        PluginManager pluginManager = null;

        try {
            NodeRegistry nodeRegistry = new NodeRegistry();

            // "plugins" 디렉토리를 바라보도록 경로 주입
            pluginManager = new PluginManager(nodeRegistry, "plugins");
            pluginManager.loadPlugins();

            FlowEngine flowEngine = new FlowEngine();

            FlowManager flowManager = new FlowManager(nodeRegistry, flowEngine);

            HttpApiServer apiServer = new HttpApiServer(8080);
            apiServer.start(flowManager);

            log.info("엔진 서버가 포트 8080에서 정상적으로 구동되었습니다.");
            log.info("종료하려면 Ctrl+C 를 누르세요.");

            PluginManager finalPluginManager = pluginManager;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("시스템 종료 신호 감지. 엔진을 안전하게 셧다운합니다...");

                apiServer.stop();
                flowManager.list().forEach(flow -> flowManager.stop(flow.getId()));

                // 엔진 종료 시 PluginClassLoader 자원 해제
                if (finalPluginManager != null) {
                    finalPluginManager.close();
                }

                log.info("엔진 종료 완료.");
            }));

            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("엔진 부팅 중 치명적 오류 발생!", e);
            if (pluginManager != null) {
                pluginManager.close();
            }
            System.exit(1);
        }
    }
}