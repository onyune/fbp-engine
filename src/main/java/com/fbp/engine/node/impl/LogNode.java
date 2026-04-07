package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogNode extends AbstractNode {
    public LogNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        System.out.println("["+time+"]["+getId()+"] "+ message.getPayload());

        send("out", message);
    }
}
