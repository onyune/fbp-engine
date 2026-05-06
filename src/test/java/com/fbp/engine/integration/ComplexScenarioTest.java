package com.fbp.engine.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.flow.SubFlowNode;
import com.fbp.engine.node.RoutingRule;
import com.fbp.engine.node.impl.CollectorNode;
import com.fbp.engine.node.impl.DeadLetterNode;
import com.fbp.engine.node.impl.DynamicRouterNode;
import com.fbp.engine.node.impl.ErrorHandlerNode;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.GeneratorNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class ComplexScenarioTest {

    @Test
    @DisplayName("Complex Scenario Integration Test")
    void testComplexScenario() throws InterruptedException {
        Flow innerFlow = new Flow("inner-alert-flow");

        FilterNode filter = new FilterNode("temp-filter", "temperature", 30.0);
        innerFlow.addNode(filter);

        SubFlowNode subFlow = new SubFlowNode("alert-subflow", innerFlow, "temp-filter");

        innerFlow.connect(filter.getId(), "out", subFlow.getBridgeNodeId(), "in");

        GeneratorNode entryNode = new GeneratorNode("entry-node");
        DynamicRouterNode router = new DynamicRouterNode("router");

        router.addRule(new RoutingRule("temperature", ">", 0.0, "tempPort"));
        router.addRule(new RoutingRule("error", "==", true, "errorPort"));

        ErrorHandlerNode errorHandler = new ErrorHandlerNode("error-handler");
        CollectorNode finalCollector = new CollectorNode("final-collector");
        DeadLetterNode dlq = new DeadLetterNode("dlq");

        Flow outerFlow = new Flow("complex-flow-1");

        outerFlow.addNode(entryNode)
                .addNode(router)
                .addNode(subFlow)
                .addNode(errorHandler)
                .addNode(finalCollector)
                .addNode(dlq)
                .connect(entryNode.getId(), "out", router.getId(), "in")
                .connect(router.getId(), "tempPort", subFlow.getId(), "in")
                .connect(router.getId(), "errorPort", errorHandler.getId(), "in")
                .connect(subFlow.getId(), "out", finalCollector.getId(), "in")
                .connect(errorHandler.getId(), "out", dlq.getId(), "in");

        FlowEngine engine = new FlowEngine();
        engine.register(outerFlow);
        engine.startFlow(outerFlow.getId());

        entryNode.generate("temperature", 35.5);
        entryNode.generate("temperature", 20.0);
        entryNode.generate("error", true);

        Thread.sleep(1000);
        engine.shutdown();

        assertEquals(1, finalCollector.getCollected().size());
        assertNotNull(dlq);
    }
}