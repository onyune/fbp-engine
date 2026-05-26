package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

public class HealthCheckerNode extends AbstractNode {
    public HealthCheckerNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        // Simple health check simulation: create new message with added fields
        Message updated = message.withEntry("status", "UP")
                                 .withEntry("check_timestamp", System.currentTimeMillis());
        send("out", updated);
    }
}
