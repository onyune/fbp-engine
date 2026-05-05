package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;

public interface BackpressureStrategy {

    /**
     * 큐에 메시지를 출가
     * @param queue 메시지를 담을 제한된 용량의 큐
     * @param message 추가할 메시지
     * @return 큐가 꽉 차서 버려진 메시지 (버려진 게 없다면 null 반환)
     */
    Message handle(BlockingQueue<Message> queue, Message message);
}
