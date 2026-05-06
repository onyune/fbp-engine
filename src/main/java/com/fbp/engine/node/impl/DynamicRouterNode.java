package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.RoutingRule;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicRouterNode extends AbstractNode {
    private final List<RoutingRule> rules = new ArrayList<>();
    private String defaultPortName = "default";

    public DynamicRouterNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort(defaultPortName);
    }

    public void addRule (RoutingRule rule){
        rules.add(rule);

        if(getOutputPort(rule.getTargetPort())==null){
            addOutputPort(rule.getTargetPort());
        }
    }

    public void setDefaultPortName(String portName){
        this.defaultPortName=portName;
        if(getOutputPort(portName)==null){
            addOutputPort(portName);
        }
    }

    @Override
    protected void onProcess(Message message) {
        for(RoutingRule rule : rules){
            if(rule.evaluate(message)){
                log.debug("[DynamicRouter {}] 규칙이 일치함,  포트 [{}]로 전송", getId(), rule.getTargetPort());
                send(rule.getTargetPort(), message);
                return;
            }
        }
        log.debug("[DynamicRouter {}] 규칙이 일치하는게 없음, 기본 포트 [{}]로 전송", getId(), defaultPortName);
        send(defaultPortName, message);

    }
}
