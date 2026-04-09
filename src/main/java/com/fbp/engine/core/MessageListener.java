package com.fbp.engine.core;

public interface MessageListener {
    // 메시지가 도착했을 때 외부 라이브러리가 호출해 줄 메서드
    void onMessage(String topic, byte[] payload);
    
    // 네트워크 연결이 끊어졌을 때 호출해 줄 메서드
    void onConnectionLost(Throwable cause);
}