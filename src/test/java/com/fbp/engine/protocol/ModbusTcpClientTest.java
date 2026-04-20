package com.fbp.engine.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModbusTcpClientTest {

    @Nested
    @DisplayName("단위 테스트 (네트워크 연결 없이 프레임 검증)")
    class UnitTest {
        private ModbusTcpClient client;
        private ByteArrayOutputStream capturedOutput;

        @BeforeEach
        void setUp() {
            client = new ModbusTcpClient("localhost", 502);
            capturedOutput = new ByteArrayOutputStream();
        }

        private void injectMockStreams(byte[] mockResponseBytes) throws Exception {
            DataOutputStream mockOut = new DataOutputStream(capturedOutput);
            DataInputStream mockIn = new DataInputStream(new ByteArrayInputStream(mockResponseBytes));

            Field outField = ModbusTcpClient.class.getDeclaredField("out");
            outField.setAccessible(true);
            outField.set(client, mockOut);

            Field inField = ModbusTcpClient.class.getDeclaredField("in");
            inField.setAccessible(true);
            inField.set(client, mockIn);
        }

        @Test
        void testReadHoldingRegistersFrame() throws Exception {
            ByteArrayOutputStream respBuf = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(respBuf);
            dos.writeShort(0);
            dos.writeShort(0);
            dos.writeShort(5);
            dos.writeByte(1);
            dos.writeByte(3);
            dos.writeByte(2);
            dos.writeShort(250);

            injectMockStreams(respBuf.toByteArray());

            int[] result = client.readHoldingRegisters(1, 10, 1);

            byte[] requestBytes = capturedOutput.toByteArray();
            assertEquals(12, requestBytes.length);

            ByteBuffer buffer = ByteBuffer.wrap(requestBytes);
            assertEquals(0, buffer.getShort());
            assertEquals(0, buffer.getShort());
            assertEquals(6, buffer.getShort());
            assertEquals(1, buffer.get());
            assertEquals(3, buffer.get());
            assertEquals(10, buffer.getShort());
            assertEquals(1, buffer.getShort());

            assertEquals(1, result.length);
            assertEquals(250, result[0]);
        }

        @Test
        void testWriteSingleRegisterFrame() throws Exception {
            ByteArrayOutputStream respBuf = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(respBuf);
            dos.writeShort(0);
            dos.writeShort(0);
            dos.writeShort(6);
            dos.writeByte(1);
            dos.writeByte(6);
            dos.writeShort(20);
            dos.writeShort(999);

            injectMockStreams(respBuf.toByteArray());

            assertDoesNotThrow(() -> {
                client.writeSingleRegister(1, 20, 999);
            });

            byte[] requestBytes = capturedOutput.toByteArray();
            assertEquals(12, requestBytes.length);

            ByteBuffer buffer = ByteBuffer.wrap(requestBytes);
            buffer.position(7);
            assertEquals(6, buffer.get());
            assertEquals(20, buffer.getShort());
            assertEquals(999, buffer.getShort());
        }

        @Test
        void testTransactionIdIncrement() throws Exception {
            ByteArrayOutputStream respBuf = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(respBuf);

            dos.writeShort(0); dos.writeShort(0); dos.writeShort(5);
            dos.writeByte(1); dos.writeByte(3); dos.writeByte(2); dos.writeShort(111);

            dos.writeShort(1); dos.writeShort(0); dos.writeShort(5);
            dos.writeByte(1); dos.writeByte(3); dos.writeByte(2); dos.writeShort(222);

            injectMockStreams(respBuf.toByteArray());

            client.readHoldingRegisters(1, 0, 1);
            client.readHoldingRegisters(1, 1, 1);

            byte[] allRequests = capturedOutput.toByteArray();
            assertEquals(24, allRequests.length);

            ByteBuffer buffer = ByteBuffer.wrap(allRequests);
            assertEquals(0, buffer.getShort(0));
            assertEquals(1, buffer.getShort(12));
        }

        @Test
        void testInitialState() {
            assertFalse(client.isConnected());
        }
    }

    @Nested
    @DisplayName("통합 테스트 (시뮬레이터 연동)")
    class IntegrationTest {
        private ModbusTcpSimulator simulator;
        private ModbusTcpClient client;

        @BeforeEach
        void setUp() throws Exception {
            simulator = new ModbusTcpSimulator(5020, 10);
            simulator.start();

            Thread.sleep(100);
            simulator.setRegister(0, 100);
            simulator.setRegister(1, 200);
            simulator.setRegister(2, 300);
            simulator.setRegister(3, 400);
            simulator.setRegister(4, 500);

            client = new ModbusTcpClient("localhost", 5020);
            client.connect();
        }

        @AfterEach
        void tearDown() {
            client.disconnect();
            simulator.stop();
        }

        @Test
        void testConnectionState() {
            assertTrue(client.isConnected());
            client.disconnect();
            assertFalse(client.isConnected());
        }

        @Test
        void testReadSingleHoldingRegister() throws Exception {
            int[] result = client.readHoldingRegisters(1, 0, 1);
            assertNotNull(result);
            assertEquals(1, result.length);
            assertEquals(100, result[0]);
        }

        @Test
        void testReadMultipleHoldingRegisters() throws Exception {
            int[] result = client.readHoldingRegisters(1, 0, 5);
            assertNotNull(result);
            assertEquals(5, result.length);
            assertArrayEquals(new int[]{100, 200, 300, 400, 500}, result);
        }

        @Test
        void testWriteSingleRegister() throws Exception {
            client.writeSingleRegister(1, 2, 999);
            assertEquals(999, simulator.getRegister(2));
        }

        @Test
        void testWriteAndReadConsistency() throws Exception {
            client.writeSingleRegister(1, 3, 777);
            int[] result = client.readHoldingRegisters(1, 3, 1);
            assertEquals(777, result[0]);
        }

        @Test
        void testErrorResponse() {
            ModbusException exception = assertThrows(ModbusException.class, () -> {
                client.readHoldingRegisters(1, 100, 1);
            });
            assertEquals(0x02, exception.getExceptionCode());
        }

        @Test
        void testSocketTimeout() throws Exception {
            client.disconnect();
            simulator.stop();
            try (java.net.ServerSocket dummyServer = new java.net.ServerSocket(5021)) {

                ModbusTcpClient timeoutClient = new ModbusTcpClient("localhost", 5021);
                timeoutClient.connect();

                assertThrows(java.net.SocketTimeoutException.class, () -> {
                    timeoutClient.readHoldingRegisters(1, 0, 1);
                }, "서버 응답이 없으므로 SocketTimeoutException이 발생");

                timeoutClient.disconnect();
            }
        }
    }
}