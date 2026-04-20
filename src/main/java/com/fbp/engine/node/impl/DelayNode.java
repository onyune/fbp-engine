package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

/**
 * input port : in
 * output port : out
 */
public class DelayNode extends AbstractNode {
    private final long delayMs;

    public DelayNode(String id, long delayMs) {
        super(id);
        this.delayMs = delayMs;
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        try{
            Thread.sleep(delayMs);//Thread로 sleep하게 되면 다른 노드들도 멈출 수 있음
            send("out", message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
