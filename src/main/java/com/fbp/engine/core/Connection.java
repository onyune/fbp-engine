package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface Connection {
    String getId();
    InputPort getTarget();
    void setTarget(InputPort target);
    //데이터 보낼때
    void deliver(Message message);
    //데이터 꺼낼때
    Message poll();
    //현재 대기 중인 메시지 수 확인 (모니터링 용도)
    int getBufferSize();

    void close();
}
