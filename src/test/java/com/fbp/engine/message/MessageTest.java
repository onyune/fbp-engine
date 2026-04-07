package com.fbp.engine.message;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageTest {
    Map<String,Object> originalData;
    Message msg;
    @BeforeEach
    void init(){
        originalData = new HashMap<>();
        originalData.put("temperature", 22.2);
        msg = new Message(originalData);
    }

    @Test
    @DisplayName("getId()가 null이 아니고 빈 문자열이 아님")
    void testCreateAutoId(){
        assertNotNull(msg.getId());
        assertFalse(msg.getId().isEmpty());

    }

    @Test
    @DisplayName("getTimestamp()가 0보다 큼")
    void getTimestamp(){
        assertTrue(msg.getTimestamp()>0);
    }

    @Test
    @DisplayName("생성 시 넣은 key-value를 get()으로 꺼낼 수 있음 / get(\"temperature\")의 반환 타입이 Double로 사용 가능")
    void keyValueTest(){
        Double temp = msg.get("temperature");
        assertEquals(22.2, temp);
    }

    @Test
    @DisplayName("get(\"없는키\")가 null 반환")
    void generic_get(){
        assertNull(msg.get("없는 키"));
    }

    @Test
    @DisplayName("getPayload().put()하면 UnsupportedOperationException 발생")
    void originalPayload_put(){
        assertThrows(UnsupportedOperationException.class, ()->{
            msg.getPayload().put("test_key","value");
        });
    }

    @Test
    @DisplayName("Message 생성에 사용한 원본 Map을 수정해도 Message 내용은 변하지 않음")
    void DontChangeOriginalMessage(){
        originalData.put("temperature", 33.3);
        assertEquals(22.2, msg.get("temperature"));
    }

    @Test
    @DisplayName("withEntry()가 반환한 Message와 원본은 서로 다른 객체 && withEntry() 후 원본 Message에 새 키가 없음"
            + "&& 새 Message에서 추가한 키의 값을 조회할 수 있음")
    void withEntryTest(){
        Message newMessage = msg.withEntry("humidity", 60.0);
        assertNotSame(msg, newMessage);
        assertFalse(msg.hasKey("humidity"));
        assertTrue(newMessage.hasKey("humidity"));
    }

    @Test
    @DisplayName("hasKey(\"temperature\")가 true &&  hasKey(\"없는키\")가 false "
            + "&& 반환된 Message에서 해당 키가 없음 && 원본 Message에는 해당 키가 여전히 있음")
    void hasKeyTest(){
        assertTrue(msg.hasKey("temperature"));
        assertFalse(msg.hasKey("없는키"));
        Message withoutKeyTest = msg.withoutKey("temperature");
        assertFalse(withoutKeyTest.hasKey("temperature"));
        assertTrue(msg.hasKey("temperature"));
    }

    @Test
    @DisplayName("toString()이 null이 아니고 payload 내용을 포함")
    void msgContainData(){
        assertNotNull(msg.toString());
        assertTrue(msg.toString().contains("temperature"));
    }
}
