package com.fbp.engine.runner;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public class PahoMqttTest {
    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:1883"; // Docker로 띄운 로컬 모스퀴토
        String clientId = "paho-test-client";      // 임의의 클라이언트 ID
        String testTopic = "sensor/test";          // 테스트용 토픽

        try {
            // 1. MqttClient 객체 생성
            MqttClient client = new MqttClient(brokerUrl, clientId);

            // 2. 이벤트 콜백 설정 (메시지 수신, 연결 완료, 끊김 등)
            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // 수신된 바이트 배열(Payload)을 문자열로 변환하여 출력
                    String payload = new String(message.getPayload());
                    System.out.println("\n>>> [메시지 수신 콜백] 토픽: " + topic + " | 내용: " + payload);
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println(">>> [연결 완료 콜백] " + serverURI);
                }

                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    System.out.println(">>> [연결 끊김 콜백] 이유: " + disconnectResponse.getReasonString());
                }

                @Override
                public void deliveryComplete(IMqttToken token) {
                    System.out.println(">>> [전송 완료 콜백]");
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {}
                @Override
                public void authPacketArrived(int reasonCode, MqttProperties properties) {}
            });

            // 3. 연결 옵션 설정 및 브로커 연결
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setCleanStart(true);
            options.setAutomaticReconnect(true); // 자동 재연결 설정

            System.out.println("1. MQTT Broker(" + brokerUrl + ") 연결 시도...");
            client.connect(options);

            // 4. 토픽 구독 (Subscribe) - QoS 1 사용
            System.out.println("2. 토픽 구독 요청: " + testTopic);
            client.subscribe(testTopic, 1);

            // 5. 토픽 발행 (Publish)
            String content = "Hello FBP! 이것은 Paho 테스트 메시지입니다.";
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(1); // 전달 보장 수준 QoS 1

            System.out.println("3. 메시지 발행 요청: " + testTopic + " -> " + content);
            client.publish(testTopic, message);

            // 콜백을 통해 메시지를 수신할 수 있도록 2초간 대기
            Thread.sleep(2000);

            // 6. 연결 해제 및 리소스 정리
            System.out.println("4. 연결 해제 및 종료");
            client.disconnect();
            client.close();

        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}