package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorHandlerNode extends AbstractNode {

    public ErrorHandlerNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        if (message.hasKey("error")) {
            Exception e = message.get("error");
            String errorNodeId = message.get("errorNodeId");

            log.warn("[ErrorHandler {}] 노드 [{}] 에서 발생한 에러 수신. 상세: {}",
                    getId(), errorNodeId, e.getMessage());

            if (message.hasKey("originalMessage")) {
                Message original = message.get("originalMessage");
                log.debug("[ErrorHandler {}] 에러를 유발한 원본 메시지: {}", getId(), original);
            }

            Message enrichedErrorMessage = message
                    .withEntry("handledBy", this.getId())
                    .withEntry("handledAt", System.currentTimeMillis())
                    .withEntry("status", "FAILED_AND_HANDLED");

            log.info("[ErrorHandler {}] 에러 기록 완료. out 포트로 전달", getId());
            send("out", enrichedErrorMessage);

        } else {
            log.warn("[ErrorHandler {}] 에러 정보가 없는 잘못된 메시지 수신: {}",
                    getId(), message.getPayload());
            send("out", message);
        }
    }
}