package com.fbp.engine.core.impl;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorPort extends DefaultOutputPort{

    public ErrorPort() {
        super("error");
    }

    @Override
    public void send(Message message) {
        if(getConnections().isEmpty()){
            Exception e = message.get("error");
            String errorNodeId = message.get("errorNodeId");
            log.error("[ErrorPort - Unhandled] 연결된 에러 핸들러가 없습니다! 노드 [{}]의 처리되지 않은 예외: {}",
                    errorNodeId, e != null ? e.getMessage() : "Unknown Error", e);
        }else{
            super.send(message);
        }
    }
}
