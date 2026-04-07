package com.fbp.engine.node;

import com.fbp.engine.message.Message;

public interface Node {
    //노드의 고유 ID를 반환하는 메서드
    String getId();
    //메시지(데이터)가 들어왔을 때 실제로 처리할 핵심 작업
    void process(Message message);

    void initialize();

    void shutdown();
}
