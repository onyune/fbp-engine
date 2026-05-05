package com.fbp.engine.core.impl;

import com.fbp.engine.core.BackpressureStrategy;
import com.fbp.engine.message.Message;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 임시버퍼에 저장하는 방식
 */
public class OverflowBufferStrategy implements BackpressureStrategy {
    private final Queue<Message> overflowBuffer = new ConcurrentLinkedQueue<>();

    @Override
    public Message handle(BlockingQueue<Message> queue, Message message) {
        if (!queue.offer(message)) {
            overflowBuffer.add(message);
        }
        return null; // 데드레터로 버려지는 메시지는 없음
    }

    public Queue<Message> getOverflowBuffer() {
        return overflowBuffer;
    }
}