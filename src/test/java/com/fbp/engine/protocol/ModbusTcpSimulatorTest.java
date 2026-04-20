package com.fbp.engine.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModbusTcpSimulator 테스트")
class ModbusTcpSimulatorTest {

    private ModbusTcpSimulator simulator;

    @BeforeEach
    void setUp() throws Exception {
        simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("1. 시작/종료 - start() 후 포트가 열리고, stop() 후 닫힘")
    void testStartAndStop() throws Exception {
        try (Socket socket = new Socket("localhost", 5020)) {
            assertTrue(socket.isConnected());
        }

        simulator.stop();
        Thread.sleep(100);

        assertThrows(Exception.class, () -> {
            new Socket("localhost", 5020);
        });
    }

    @Test
    @DisplayName("2. 레지스터 초기값 - setRegister() 후 getRegister()로 설정 값 확인")
    void testRegisterInitialization() {
        simulator.setRegister(0, 1234);
        simulator.setRegister(5, 5678);

        assertEquals(1234, simulator.getRegister(0));
        assertEquals(5678, simulator.getRegister(5));
        assertEquals(0, simulator.getRegister(1));
    }

    @Test
    @DisplayName("3. FC 03 응답 - ModbusTcpClient로 읽기 요청 시 설정된 레지스터 값이 응답됨")
    void testFC03Response() throws Exception {
        simulator.setRegister(2, 500);

        ModbusTcpClient client = new ModbusTcpClient("localhost", 5020);
        client.connect();

        int[] result = client.readHoldingRegisters(1, 2, 1);

        assertEquals(1, result.length);
        assertEquals(500, result[0]);

        client.disconnect();
    }

    @Test
    @DisplayName("4. FC 06 응답 - ModbusTcpClient로 쓰기 요청 시 레지스터 값이 변경되고 에코백 응답")
    void testFC06Response() throws Exception {
        ModbusTcpClient client = new ModbusTcpClient("localhost", 5020);
        client.connect();

        client.writeSingleRegister(1, 3, 999);

        assertEquals(999, simulator.getRegister(3));

        client.disconnect();
    }

    @Test
    @DisplayName("5. 잘못된 주소 에러 - 범위를 벗어난 주소 요청 시 Exception Code 0x02 에러 응답")
    void testIllegalDataAddressError() throws Exception {
        ModbusTcpClient client = new ModbusTcpClient("localhost", 5020);
        client.connect();

        ModbusException exception = assertThrows(ModbusException.class, () -> {
            client.readHoldingRegisters(1, 10, 1);
        });

        assertEquals(0x02, exception.getExceptionCode());

        client.disconnect();
    }

    @Test
    @DisplayName("6. 다중 클라이언트 - 2개 클라이언트가 동시 접속하여 독립적으로 요청/응답 가능")
    void testMultipleClients() throws Exception {
        ModbusTcpClient client1 = new ModbusTcpClient("localhost", 5020);
        client1.connect();

        ModbusTcpClient client2 = new ModbusTcpClient("localhost", 5020);
        client2.connect();

        client1.writeSingleRegister(1, 4, 777);

        int[] result = client2.readHoldingRegisters(1, 4, 1);

        assertEquals(777, result[0]);

        client1.disconnect();
        client2.disconnect();
    }
}