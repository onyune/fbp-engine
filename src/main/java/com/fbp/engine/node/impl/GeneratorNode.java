package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import java.util.Map;

/**
 * input port : X
 * output port : out
 */
public class GeneratorNode extends AbstractNode {


    public GeneratorNode(String id) {
        super(id);
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {

    }

    // key, value를 가지고 메시지를 만들어서 연결된 outputPort의 send함
    public void generate(String key, Object value){
        Map<String, Object> payload = new HashMap<>();
        payload.put(key,value);
        send("out",new Message(payload));
    }
}
