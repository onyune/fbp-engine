package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.Setter;

public class BackpressureConnection extends Connection{
    @Getter
    private final BlockingQueue<Message> queue;
    @Getter @Setter
    private volatile BackpressureStrategy strategy;

    @Getter
    private final AtomicLong dropCount = new AtomicLong(0);
    @Setter
    private InputPort deadLetterPort;

    public BackpressureConnection(String id, int capacity, BackpressureStrategy strategy) {
        super(id, capacity);
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.strategy = strategy;
    }

    /**
     * 노드 (outputPort)에서 메시지를 쏠 때 이 메서드 호출
     * @param message
     */
    public void send(Message message){
        Message droppedMessage = strategy.handle(queue, message);
        if (droppedMessage != null) {
            dropCount.incrementAndGet();
            if (deadLetterPort != null) {
                deadLetterPort.receive(droppedMessage);
            }
        }
    }

    /**
     * 큐에 쌓인 메시지를 진짜 타겟 노드로 밀어내는 역할 (동기식 테스트용)
     */
    public void flush() {
        while (!queue.isEmpty()) {
            Message m = queue.poll();
            if (m != null && getTarget() != null) {
                getTarget().receive(m);
            }
        }
    }

}
