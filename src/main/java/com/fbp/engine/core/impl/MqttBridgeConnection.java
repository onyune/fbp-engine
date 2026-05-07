package com.fbp.engine.core.impl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.MessageSerializer;
import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

@Slf4j
public class MqttBridgeConnection implements Connection {
    private static final int DEFAULT_BUFFER_SIZE=100;

    @Getter
    private final String id;

    @Getter
    @Setter
    private InputPort target;

    private final BlockingQueue<Message> internalQueue;
    private final String brokerUrl;
    private final String topic;
    private final int qos;
    private final MessageSerializer serializer;

    private MqttClient publisher;
    private MqttClient subscriber;

    public MqttBridgeConnection(String id, String brokerUrl, String topic, MessageSerializer serializer,
                                int qos) {
        this.id = id;
        this.internalQueue = new LinkedBlockingQueue<>(DEFAULT_BUFFER_SIZE);
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.serializer = serializer;
        this.qos = qos;
        connect();
    }

    @Override
    public void deliver(Message message) {
        try {
            byte[] payload = serializer.serialize(message);
            MqttMessage mqttMsg = new MqttMessage(payload);
            mqttMsg.setQos(qos);

            publisher.publish(topic, mqttMsg);
        } catch (Exception e) {
            log.error("[MqttBridge {}] 메시지 직렬화/발행 실패", id, e);
        }
    }

    @Override
    public Message poll() {
        try{
            return internalQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public int getBufferSize() {
        return internalQueue.size();
    }

    @Override
    public void close() {
        internalQueue.clear();
        try{
            if(subscriber != null && subscriber.isConnected()){
                subscriber.disconnect();
                subscriber.close();
            }
            if(publisher != null && publisher.isConnected()){
                publisher.disconnect();
                publisher.close();
            }

        }catch (MqttException e){
            log.error("[MqttBridge {}] 종료 중 에러 발생", id, e);
        }
    }

    private void connect(){
        try{
            String pubClientId = "pub-"+ id+"-"+System.nanoTime();
            publisher = new MqttClient(brokerUrl, pubClientId);
            MqttConnectionOptions pubOpts = new MqttConnectionOptions();
            pubOpts.setCleanStart(true);
            pubOpts.setAutomaticReconnect(true);
            publisher.connect(pubOpts);
            String subClientId = "sub-"+id+"-"+System.nanoTime();
            subscriber = new MqttClient(brokerUrl, subClientId);

            subscriber.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
                    log.warn("[MqttBridge {}] 수신 연결 끊김", id);
                }

                @Override
                public void mqttErrorOccurred(MqttException e) {

                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    try {
                        Message msg = serializer.deserialize(mqttMessage.getPayload());

                        internalQueue.put(msg);
                    } catch (Exception e) {
                        log.error("[MqttBridge {}] 메시지 역직렬화 실패: {}", id, e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttToken iMqttToken) {

                }

                @Override
                public void connectComplete(boolean b, String s) {
                    try {
                        // 자동 재연결 시 구독도 다시 해줌
                        subscriber.subscribe(MqttBridgeConnection.this.topic, qos);
                        log.info("[MqttBridge {}] 브로커 (재)연결 및 토픽 구독 완료: {}", id, MqttBridgeConnection.this.topic);
                    } catch (MqttException e) {
                        log.error("구독 재설정 실패", e);
                    }
                }

                @Override
                public void authPacketArrived(int i, MqttProperties mqttProperties) {

                }
            });

            MqttConnectionOptions subOpts = new MqttConnectionOptions();
            subOpts.setCleanStart(true);
            subOpts.setAutomaticReconnect(true);
            subscriber.connect(subOpts);
        }catch (MqttException e){
            log.error("[MqttBridge {}] 브로커 접속 실패. url={}", id, brokerUrl, e);
            throw new RuntimeException("MQTT Bridge 초기화 실패", e);
        }
    }
}
