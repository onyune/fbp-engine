package com.fbp.engine.core.impl;

import com.fbp.engine.core.BackpressureStrategy;
import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;

public class DropOldestStrategy implements BackpressureStrategy {
    @Override
    public Message handle(BlockingQueue<Message> queue, Message message) {
        if(!queue.offer(message)){
            Message oldest = queue.poll();
            queue.offer(message);

            return oldest; // DeadLetterQueue로 보내기 위해 반환
        }
        return null;
    }
}
