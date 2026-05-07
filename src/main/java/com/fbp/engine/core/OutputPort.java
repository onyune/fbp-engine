package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import java.util.List;

public interface OutputPort {
    // 포트 이름 (예: "out", "error")
    String getName();
    // Connection을 연결
    void connect(Connection connection);
    // 동적 연결 삭제를 위해 포트에서 해당 커넥션을 분리하는 기능이 필요
    void disconnect(Connection connection);
    // 연결된 모든 Connection으로 메시지 전송
    void send(Message message);

    List<Connection> getConnections();
}
