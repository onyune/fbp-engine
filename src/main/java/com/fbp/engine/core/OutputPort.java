package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface OutputPort {
    // 포트 이름 (예: "out", "error")
    String getName();
    // Connection을 연결
    void connect(Connection connection);
    // 연결된 모든 Connection으로 메시지 전송
    void send(Message message);
}
