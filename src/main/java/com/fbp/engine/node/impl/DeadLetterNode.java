package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * InputPort: in
 * 처리 불가 메시지 최종 저장
 */
public class DeadLetterNode extends AbstractNode {

    private final List<Message> droppedMessages = new ArrayList<>();

    public DeadLetterNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        droppedMessages.add(message);
    }
    public List<Message> getDroppedMessages(){
        return Collections.unmodifiableList(droppedMessages);
    }

}
