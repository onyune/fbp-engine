package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollectorNode extends AbstractNode {
    
    private final List<Message> collected = new CopyOnWriteArrayList<>();

    public CollectorNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        collected.add(message);
    }

    public List<Message> getCollected() {
        return collected;
    }
}