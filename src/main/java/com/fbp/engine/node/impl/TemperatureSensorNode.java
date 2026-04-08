package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import java.util.Map;

/**
 * input port : trigger
 * output port : out
 */
public class TemperatureSensorNode extends AbstractNode {
    private final double min;
    private final double max;

    public TemperatureSensorNode(String id,double min, double max) {
        super(id);
        this.min = min;
        this.max = max;
        addInputPort("trigger");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        double rawTemp = min + Math.random() * (max - min);
        double temp = Math.round(rawTemp * 10.0) / 10.0;
        Map<String, Object> payload = new HashMap<>();
        payload.put("sensorId", getId());
        payload.put("temperature", temp);
        payload.put("unit", "°C");
        payload.put("timestamp", System.currentTimeMillis());
        Message msg = new Message(payload);
        send("out", msg);
    }
}
