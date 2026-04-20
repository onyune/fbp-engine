package com.fbp.engine.node.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ProtocolNode;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;

/**
 * input port : in
 * output port : X
 */
@Slf4j
public class MqttPublisherNode extends ProtocolNode {
    private MqttClient client;
    private final ObjectMapper objectMapper;

    private String brokerUrl;
    private String clientId;
    private String topic;
    private int qos;
    private boolean retained;
    private int errorCount=0;

    public MqttPublisherNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("in");

        this.objectMapper = new ObjectMapper();

        this.brokerUrl = (String) getConfig("brokerUrl");
        this.clientId = (String) getConfig("clientId");
        this.topic = (String) getConfig("topic");
        Object qosObj = getConfig("qos");
        this.qos = (qosObj instanceof Number) ? ((Number) qosObj).intValue() : 1;

        Object retainedObj =getConfig("retained");
        this.retained = retainedObj instanceof Boolean;

        if (this.brokerUrl == null || this.clientId == null || this.topic == null) {
            throw new IllegalArgumentException("[" + id + "] brokerUrl, clientId, topic 설정은 필수입니다.");
        }
    }

    @Override
    protected void connect() throws Exception {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        options.setAutomaticReconnect(true);

        client = new MqttClient(brokerUrl, clientId);
        log.info("[{}] MQTT Broker({}) 연결 시도", getId(), brokerUrl);
        client.connect(options);
        log.info("[{}] MQTT Broker 연결 완료 (Publisher)", getId());

    }

    @Override
    protected void disconnect() throws Exception {
        if(client!=null && client.isConnected()){
            client.disconnect();
            client.close();
        }
    }

    @Override
    protected void onProcess(Message message) {
        if(client==null || !client.isConnected()) {
            log.warn("[{}] 브로커와 연결되어 있지 않아 메시지 발행할 수 없음 / 에러누적: {}", getId(), ++errorCount);
        }
        try{
            Map<String, Object> payload = message.getPayload();
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);

            String targetTopic = this.topic;

            if(payload.containsKey("topic") && payload.get("topic") instanceof String){
                targetTopic = (String) payload.get("topic");
            }

            if(targetTopic == null || targetTopic.trim().isEmpty()){
                log.warn("[{}] 발행할 토픽이 지정되지않음 메시지 무시", getId());
                return;
            }

            MqttMessage mqttMessage = new MqttMessage(payloadBytes);
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);

            client.publish(targetTopic, mqttMessage);
        } catch (Exception e) {
            errorCount++;
            log.error("[{}] 메시지 발행 실패 (에러 누적: {}): {}", getId(), errorCount, e.getMessage());
        }


    }
}
