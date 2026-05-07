package com.fbp.engine.core.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.core.MessageSerializer;
import com.fbp.engine.message.Message;
import java.io.IOException;

public class JsonMessageSerializer implements MessageSerializer {
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(Message message) {
        try {
            // Message 객체를 JSON 문자열 바이트로 변환
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }

    @Override
    public Message deserialize(byte[] bytes) {
        try {
            // JSON 바이트를 다시 Message 객체로 복원
            return objectMapper.readValue(bytes, Message.class);
        } catch (IOException e) {
            throw new RuntimeException("메시지 역직렬화 실패", e);
        }
    }
}