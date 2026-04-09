package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * input port : in-1 / in-2
 * output port : out
 */
@Slf4j
public class MergeNode extends AbstractNode {
    private Message pending1;
    private Message pending2;

    public MergeNode(String id) {
        super(id);
        addInputPort("in-1");
        addInputPort("in-2");
        addOutputPort("out");
    }


    @Override
    protected synchronized void onProcess(Message message) {
        String portName = message.get("__inPort");

        if("in-1".equals(portName)){
            pending1=message;
        } else if ("in-2".equals(portName)) {
            pending2=message;
        }else {
            log.warn("[{}] 알 수 없는 포트로 메시지 수신", getId());
            return;
        }

        if(pending1 != null && pending2 !=null){
            Map<String, Object> mergedPayload = new HashMap<>();

            for(Map.Entry<String, Object> entry : pending1.getPayload().entrySet()){
                mergedPayload.put("in1_" + entry.getKey(), entry.getValue());
            }


            for(Map.Entry<String, Object> entry : pending2.getPayload().entrySet()){
                mergedPayload.put("in2_" + entry.getKey(), entry.getValue());
            }

            mergedPayload.remove("in1___inPort");
            mergedPayload.remove("in2___inPort");

            mergedPayload.put("mergedBy", getId());

            Message mergedMsg = new Message(mergedPayload);
            send("out", mergedMsg);

            pending1=null;
            pending2=null;
        }
    }
}
