package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface InputPort {
    // 포트이름(예: "in", "trigger")
    String getName();

    // Connection으로부터 메시지를 수신
    void receive(Message message);
}
