package com.fbp.engine.core.impl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class DefaultOutputPort implements OutputPort {
    @Getter
    private final String name;
    private final List<Connection> connections = new ArrayList<>();

    public DefaultOutputPort(String name) {
        this.name = name;
    }

    // 커넥션을 리스트에 추가
    @Override
    public void connect(Connection connection) {
        connections.add(connection);
    }

    // 모든 Connection의 deliver 호출
    @Override
    public void send(Message message) {
        for(Connection conn: connections){
            conn.deliver(message);
        }
    }
}
