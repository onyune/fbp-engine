package com.fbp.engine.core;

import com.fbp.engine.core.impl.LocalConnection;
import com.fbp.engine.message.Message;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.Setter;

public class BackpressureConnection extends LocalConnection {
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
        // 주입된 전략 객체에게 처리를 위임
        Message droppedMessage = strategy.handle(queue, message);
        // 만약 전략에 의해 메시지가 버려졌다면
        if (droppedMessage != null) {
            dropCount.incrementAndGet(); // 버려진 카운트 증가 (메트릭 수집용)
            // DLQ 포트가 설정되어 있다면 버려진 메시지를 살려냄
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
