package com.fbp.engine.core.impl;

import com.fbp.engine.core.BackpressureStrategy;
import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;

public class DropNewestStrategy implements BackpressureStrategy {
    @Override
    public Message handle(BlockingQueue<Message> queue, Message message) {
        if(!queue.offer(message)){
            return message; // deadLetterQueue로 보내기위해 반환
        }
        return null;
    }
}
