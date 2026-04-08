package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

/**
 * input port : in
 * output port : alert / normal
 */
public class ThresholdFilterNode extends AbstractNode {
    private final String fieldName;
    private final double threshold;

    public ThresholdFilterNode(String id, String fieldName, double threshold) {
        super(id);
        this.fieldName = fieldName;
        this.threshold = threshold;
        addInputPort("in");
        addOutputPort("alert");
        addOutputPort("normal");
    }

    @Override
    protected void onProcess(Message message) {
        if(message.hasKey(fieldName)){
            Object fieldNameObject = message.get(fieldName);
            if(fieldNameObject instanceof Number && ((Number) fieldNameObject).doubleValue() > threshold){
                send("alert", message);
            }else{
                send("normal", message);
            }
        }

    }
}
