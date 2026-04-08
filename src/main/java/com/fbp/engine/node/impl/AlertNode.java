package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

/**
 * input port : in
 * output port : X
 */
@Slf4j
public class AlertNode extends AbstractNode {
    public AlertNode(String id) {
        super(id);
        addInputPort("in");
        //outputPort 없음 경고 출력만 수행
    }

    @Override
    protected void onProcess(Message message) {
        if(message.hasKey("temperature")) {
            log.info("[경고] 온도 센서 {} : {}°C — 임계값 초과!", message.get("sensorId"), message.get("temperature"));
        } else if (message.hasKey("humidity")) {
            log.info("[경고] 습도 센서 {} : {}% — 임계값 초과!", message.get("sensorId"), message.get("humidity"));
        } else {
            log.warn("[경고] 알 수 없는 센서 데이터: {}", message.getPayload());
        }
    }
}
