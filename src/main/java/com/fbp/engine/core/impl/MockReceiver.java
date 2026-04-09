package com.fbp.engine.core.impl;

import com.fbp.engine.core.MessageListener;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;

// 외부 데이터를 받는다고 가정하는 가상의 수신 클래스
public class MockReceiver implements MessageListener {

    @Override
    public void onMessage(String topic, byte[] payload) {
        System.out.println("--- 외부 메시지 수신 ---");
        
        // 수신한 날것의 byte[]를 우리가 읽을 수 있는 String으로 변환
        String rawData = new String(payload);
        System.out.println("원본 토픽: " + topic);
        System.out.println("원본 데이터: " + rawData);

        Map<String,Object> data = new HashMap<>();
        data.put("topic", topic);
        data.put("payload", rawData);

        // FBP 엔진 규격에 맞는 빈 Message 객체 생성
        Message fbpMessage = new Message(data);

        // 다음 노드로 전송 (실제 노드 상속 구조라면 send("out", fbpMessage) 호출)
        System.out.println("FBP Message로 변환 완료: " + fbpMessage.toString());
        
        // send("out", fbpMessage); // 실제로는 이런 식으로 엔진의 OutputPort를 태움
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        System.err.println("연결 끊김: " + cause.getMessage());
    }
}