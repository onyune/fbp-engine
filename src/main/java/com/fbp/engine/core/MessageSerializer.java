package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface MessageSerializer {
    // Message 객체를 byte[] 로 변환 (송신용)
    byte[] serialize(Message message);
    
    // byte[] 를 다시 Message 객체로 복원 (수신용)
    Message deserialize(byte[] bytes);
}