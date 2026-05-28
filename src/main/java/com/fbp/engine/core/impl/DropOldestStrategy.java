package com.fbp.engine.core.impl;

import com.fbp.engine.core.BackpressureStrategy;
import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;

// 가장 오래된 데이터 버리기 (최신 상태가 가장 중요한 IoT 센서같은거에 활용)
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
