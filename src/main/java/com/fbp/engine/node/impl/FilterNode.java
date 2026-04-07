package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

public class FilterNode extends AbstractNode {

    private final String key;
    private final Double threshold;

    public FilterNode(String id,String key, double threshold) {
        super(id);
        this.key=key;
        this.threshold=threshold;
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        if(message.hasKey(key)){
            Object o = message.get(key);
            if(o instanceof Number && ((Number) o).doubleValue() >= threshold){
                send("out", message);
            }
        }
    }
}
