package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

/**
 * input port : in
 * output port : out
 */
public class CounterNode extends AbstractNode {
    private int count = 0;

    public CounterNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        count++;
        Message newMessage = message.withEntry("count", count);
        send("out", newMessage);
    }

    @Override
    public void shutdown() {
        System.out.println("["+getId()+"] 총 처리 메시지: "+ count+"건");
        super.shutdown();
    }
}
