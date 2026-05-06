package com.fbp.engine.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.ThreadPoolConfig;
import com.fbp.engine.engine.test.LoadTester;
import com.fbp.engine.engine.test.MemoryMonitor;
import com.fbp.engine.engine.test.PerformanceResult;
import com.fbp.engine.node.impl.DynamicRouterNode;
import com.fbp.engine.node.impl.TransformNode;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("performance")
class LoadTest {

    @Test
    @DisplayName("Load and Performance Test")
    void testEngineUnderExtremeLoad() throws Exception {
        MemoryMonitor memoryMonitor = new MemoryMonitor();

        ThreadPoolConfig poolConfig = new ThreadPoolConfig();
        ThreadPoolExecutor executor = poolConfig.createExecutor();

        DynamicRouterNode entryNode = new DynamicRouterNode("entry-router");
        int testMessageCount = 10000;
        LoadTester loadTester = new LoadTester(entryNode, testMessageCount);

        TransformNode exitNode = new TransformNode("exit-node", msg -> {
            loadTester.recordCompletion(msg, true);
            return msg;
        });

        Flow flow = new Flow("load-test-flow");
        
        flow.addNode(entryNode)
            .addNode(exitNode)
            .connect(entryNode.getId(), "default", exitNode.getId(), "in");

        FlowEngine engine = new FlowEngine();
        engine.register(flow);
        engine.startFlow(flow.getId());

        memoryMonitor.start(100);

        PerformanceResult result = loadTester.run();

        memoryMonitor.stop();
        engine.shutdown();
        executor.shutdown();

        assertTrue(result.getThroughputTps() >= 1000.0);
        assertTrue(result.getAverageLatencyMs() <= 10.0);
        assertFalse(memoryMonitor.isMonotonicallyIncreasing());
    }
}