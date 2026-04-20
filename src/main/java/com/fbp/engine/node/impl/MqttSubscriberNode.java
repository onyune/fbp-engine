package com.fbp.engine.node.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ProtocolNode;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttMessage;

/**
 * input port : X
 * output port : out
 */
@Slf4j
public class MqttSubscriberNode extends ProtocolNode {
    private MqttClient client;
    private final ObjectMapper objectMapper;

    private String brokerUrl;
    private String clientId;
    private String topic;
    private int qos;

    public MqttSubscriberNode(String id, Map<String, Object> config) {
        super(id, config);
        addOutputPort("out");
        this.objectMapper = new ObjectMapper();
        this.brokerUrl = (String) getConfig("brokerUrl");
        this.clientId = (String) getConfig("clientId");
        this.topic = (String) getConfig("topic");
        Object qosObj = getConfig("qos");
        this.qos = (qosObj instanceof Number) ? ((Number) qosObj).intValue() : 1;
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
        client.setCallback(new MqttCallback() {
            /**
             * 구독한 topic으로 누군가 메시지를 publish(발행)해서, 브로커가 데이터를 밀어주었을 때 호출됨
             * @param incomingTopic
             * @param mqttMessage
             */
            @Override
            public void messageArrived(String incomingTopic, MqttMessage mqttMessage) {
                // 들어온 mqttMessage payload를 String으로 변환
                String payloadStr = new String(mqttMessage.getPayload());
                Map<String, Object> payloadMap;

                try {
                    // JSON 파싱 Map으로 변환
                    payloadMap = objectMapper.readValue(payloadStr, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    // JSON 파싱 실패 시 예외 처리
                    payloadMap = new HashMap<>();
                    payloadMap.put("rawPayload", payloadStr);
                }

                // 토픽과 시간 정보 추가
                payloadMap.put("topic", incomingTopic);
                payloadMap.put("mqttTimestamp", System.currentTimeMillis());

                // FBP엔진이 알아들을 수 있는 Message로 변환하여 out 포트로 전송
                Message message = new Message(payloadMap);
                send("out", message);

                log.info("[{}] 메시지 수신 및 전송 완료: {}", getId(), message);
            }

            @Override
            public void disconnected(MqttDisconnectResponse response) {
                log.warn("[{}] MQTT Broker 연결 끊김: {}", getId(), response.getReasonString());
            }

            /**
             * 자동 재연결에 성공했을 때
             * @param reconnect 재연결 플래그
             * @param serverURI brokerUrl
             */
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                log.info("[{}] MQTT Broker 연결 완료", getId());
            }

            // 나머지 필수 오버라이드 메서드들
            @Override
            public void deliveryComplete(org.eclipse.paho.mqttv5.client.IMqttToken token) {}
            @Override
            public void mqttErrorOccurred(org.eclipse.paho.mqttv5.common.MqttException exception) {}
            @Override
            public void authPacketArrived(int reasonCode, org.eclipse.paho.mqttv5.common.packet.MqttProperties properties) {}
        });
        client.connect(options);
        client.subscribe(topic,qos);
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

    }
}
