package com.fbp.engine.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModbusException 테스트")
class ModbusExceptionTest {

    @Test
    @DisplayName("1. getMessage 포맷")
    void testGetMessageFormat() {
        int functionCode = 0x03;
        int exceptionCode = 0x02;

        ModbusException exception = new ModbusException(functionCode, exceptionCode);
        String message = exception.getMessage();

        assertTrue(message.toLowerCase().contains("0x03"), "메시지에 functionCode가 포함되어야 합니다.");
        assertTrue(message.toLowerCase().contains("0x02"), "메시지에 exceptionCode가 포함되어야 합니다.");
    }

    @Test
    @DisplayName("2. getExceptionCode")
    void testGetExceptionCode() {
        ModbusException exception = new ModbusException(0x06, 0x04);

        assertEquals(0x04, exception.getExceptionCode(), "지정한 exceptionCode가 정확히 반환되어야 합니다.");
    }

    @Test
    @DisplayName("3. 상수 값")
    void testConstants() throws Exception {

        assertEquals(0x01, getPrivateConstant("ILLEGAL_FUNCTION"));
        assertEquals(0x02, getPrivateConstant("ILLEGAL_DATA_ADDRESS"));
        assertEquals(0x03, getPrivateConstant("ILLEGAL_DATA_VALUE"));
        assertEquals(0x04, getPrivateConstant("SLAVE_DEVICE_FAILURE"));
    }

    private int getPrivateConstant(String fieldName) throws Exception {
        Field field = ModbusException.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int) field.get(null);
    }
}