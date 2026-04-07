package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Getter;
import lombok.Setter;

public class Connection {
    private static final int DEFAULT_BUFFER_SIZE=100;
    @Getter
    private final String id;
    @Getter
    @Setter //도착지 설정
    private InputPort target;
    private final BlockingQueue<Message> buffer;


    public Connection(String id, int capacity) {
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>(capacity);
    }

    public Connection(String id) {
        this(id, DEFAULT_BUFFER_SIZE);
    }

    //생산자용: 데이터를 큐에 넣기만 함 (목적지로 바로 쏘지 않음)
    public void deliver(Message message){
        try{
            buffer.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    //소비자용: 큐에서 데이터를 꺼냄
    public Message poll(){
        try{
            return buffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // 현재 버퍼 크기
    public int getBufferSize(){
        return buffer.size();
    }


}
