package com.fbp.engine.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Message {
    private final String id;
    private final Map<String, Object> payload; //실제 데이터가 담기는 맵
    private final long timestamp;

    //처음 메시지를 만들 때 사용
    public Message(Map<String, Object> payload) {
        this.id = UUID.randomUUID().toString(); //고유 Id 자동생성
        this.payload = Collections.unmodifiableMap(new HashMap<>(payload)); // 복사본을 만들어 외부 수정 차단
        this.timestamp = System.currentTimeMillis(); //현재 시간 기록
    }

    //내부용 생성자 : withEntry 등에서 기존 Id와 시간을 유지한 채 복사본을 만들 때
    private Message(String id, Map<String, Object> payload, long timestamp) {
        this.id = id;
        this.payload = Collections.unmodifiableMap(payload);
        this.timestamp = timestamp;
    }
    // ex: Double temp = message.get("temperature")
    @SuppressWarnings("unchecked")
    public <T> T get(String key){
        return (T) payload.get(key);
    }

    //키 존재 여부 확인
    public boolean hasKey(String key){
        return payload.containsKey(key);
    }

    //기존 페이로드에 항목을 추가한 새 Message 반환 (원본 불변)
    public Message withEntry(String key, Object value){
        Map<String, Object> newPayload = new HashMap<>(payload);
        newPayload.put(key, value);
        return new Message(this.id, newPayload, this.timestamp);
    }

    //특정 키를 제거한 새 Message 반환
    public Message withoutKey(String key){
        Map<String, Object> newPayload = new HashMap<>(payload);
        newPayload.remove(key);
        return new Message(this.id, newPayload, this.timestamp);
    }

}
