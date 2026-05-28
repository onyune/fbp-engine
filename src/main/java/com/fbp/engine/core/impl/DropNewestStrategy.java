package com.fbp.engine.core.impl;

import com.fbp.engine.core.BackpressureStrategy;
import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;

// 최신 데이터 버리기 (큐가 여유를 찾을 때까지 들어오는 요청 무시)
public class DropNewestStrategy implements BackpressureStrategy {
    @Override
    public Message handle(BlockingQueue<Message> queue, Message message) {
        if(!queue.offer(message)){
            return message; // deadLetterQueue로 보내기위해 반환
        }
        return null;
    }
}
