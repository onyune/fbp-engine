package com.fbp.engine.flow;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

import java.util.List;

public class SubFlowNode extends AbstractNode {

    private final Flow innerFlow;
    private final String entryNodeId;
    private final FlowEngine innerEngine;

    public SubFlowNode(String id, Flow innerFlow, String entryNodeId) {
        super(id);
        this.innerFlow = innerFlow;
        this.entryNodeId = entryNodeId;
        this.innerEngine = new FlowEngine();

        addInputPort("in");
        addOutputPort("out");

        BridgeOutNode bridgeOut = new BridgeOutNode(getBridgeNodeId());
        this.innerFlow.addNode(bridgeOut);

        BridgeErrorNode bridgeError = new BridgeErrorNode(getBridgeErrorNodeId());
        this.innerFlow.addNode(bridgeError);

        List<AbstractNode> nodes = innerFlow.getNodes();
        for (AbstractNode node : nodes) {
            if (node != bridgeOut && node != bridgeError) {
                innerFlow.connect(node.getId(), "error", bridgeError.getId(), "in");
            }
        }

        innerEngine.register(innerFlow);
    }

    @Override
    protected void onProcess(Message message) {
        AbstractNode entryNode = innerFlow.getNodes().stream()
                .filter(node -> node.getId().equals(entryNodeId))
                .findFirst()
                .orElse(null);

        if (entryNode != null) {
            entryNode.process(message);
        } else {
            throw new IllegalArgumentException(entryNodeId);
        }
    }

    private class BridgeOutNode extends AbstractNode {
        public BridgeOutNode(String id) {
            super(id);
            addInputPort("in");
        }

        @Override
        protected void onProcess(Message message) {
            SubFlowNode.this.send("out", message);
        }
    }

    private class BridgeErrorNode extends AbstractNode {
        public BridgeErrorNode(String id) {
            super(id);
            addInputPort("in");
        }

        @Override
        protected void onProcess(Message message) {
            SubFlowNode.this.send("error", message);
        }
    }

    public String getBridgeNodeId() {
        return getId() + "-bridge-out";
    }

    public String getBridgeErrorNodeId() {
        return getId() + "-bridge-error";
    }

    @Override
    public void initialize() {
        innerEngine.startFlow(innerFlow.getId());
    }

    @Override
    public void shutdown() {
        innerEngine.stopFlow(innerFlow.getId());
    }
}