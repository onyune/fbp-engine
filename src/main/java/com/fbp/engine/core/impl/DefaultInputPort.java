package com.fbp.engine.core.impl;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import lombok.Getter;

public class DefaultInputPort implements InputPort {
    @Getter
    private final String name;
    private final Node owner; // 이 포트를 달고 있는 주인 노드

    public DefaultInputPort(String name, Node owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public void receive(Message message) {
        Message taggedMessage = message.withEntry("__inPort", this.getName());
        owner.process(taggedMessage);
    }
}
