package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;

/**
 * input port : in
 * output port : X
 */
public class PrintNode extends AbstractNode {
    public PrintNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        System.out.println("["+getId()+"] "+message.getPayload());
    }
}
