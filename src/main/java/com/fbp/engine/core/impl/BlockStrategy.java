package com.fbp.engine.core.impl;

import com.fbp.engine.core.BackpressureStrategy;
import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;

public class BlockStrategy implements BackpressureStrategy {
    @Override
    public Message handle(BlockingQueue<Message> queue, Message message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("큐 대기 중 인터럽트가 발생했습니다.", e);
        }
        return null;
    }
}