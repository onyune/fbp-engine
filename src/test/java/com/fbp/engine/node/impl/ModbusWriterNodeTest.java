package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.ConnectionState;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModbusWriterNode 테스트")
class ModbusWriterNodeTest {

    @Nested
    @DisplayName("단위 테스트")
    class UnitTest {
        private ModbusWriterNode node;

        @BeforeEach
        void setUp() {
            Map<String, Object> config = new HashMap<>();
            config.put("host", "localhost");
            config.put("port", 5020);
            config.put("slaveId", 1);
            config.put("registerAddress", 2);
            config.put("valueField", "alertCode");

            node = new ModbusWriterNode("writer", config);
        }

        @Test
        void testPorts() {
            assertNotNull(node.getInputPort("in"));
            assertNotNull(node.getOutputPort("result"));
        }

        @Test
        void testInitialState() {
            assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
            assertFalse(node.isConnected());
        }

        @Test
        void testConfig() {
            assertEquals("localhost", node.getConfig("host"));
            assertEquals(2, node.getConfig("registerAddress"));
            assertEquals("alertCode", node.getConfig("valueField"));
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {
        private ModbusTcpSimulator simulator;

        @BeforeEach
        void setUp() throws Exception {
            simulator = new ModbusTcpSimulator(5020, 10);
            simulator.start();
            Thread.sleep(100);

            simulator.setRegister(2, 0);
        }

        @AfterEach
        void tearDown() {
            if (simulator != null) {
                simulator.stop();
            }
        }

        private Map<String, Object> createBaseConfig() {
            Map<String, Object> config = new HashMap<>();
            config.put("host", "localhost");
            config.put("port", 5020);
            config.put("slaveId", 1);
            config.put("registerAddress", 2);
            config.put("valueField", "alertCode");
            config.put("reconnectIntervalMs", 100);
            config.put("maxRetries", 3);
            return config;
        }

        @Test
        void testConnectionSuccess() {
            Map<String, Object> config = createBaseConfig();
            ModbusWriterNode node = new ModbusWriterNode("writer", config);

            node.initialize();

            assertTrue(node.isConnected());
            assertEquals(ConnectionState.CONNECTED, node.getConnectionState());

            node.shutdown();
        }

        @Test
        void testWriteRegistersWithoutScale() throws Exception {
            Map<String, Object> config = createBaseConfig();
            ModbusWriterNode node = new ModbusWriterNode("writer", config);

            node.initialize();

            Map<String, Object> payload = new HashMap<>();
            payload.put("alertCode", 150);
            node.process(new Message(payload));

            Thread.sleep(100);

            assertEquals(150, simulator.getRegister(2));

            node.shutdown();
        }

        @Test
        void testWriteRegistersWithScale() throws Exception {
            Map<String, Object> config = createBaseConfig();
            config.put("scale", 10.0);
            ModbusWriterNode node = new ModbusWriterNode("writer", config);

            node.initialize();

            Map<String, Object> payload = new HashMap<>();
            payload.put("alertCode", 25.5);
            node.process(new Message(payload));

            Thread.sleep(100);

            assertEquals(255, simulator.getRegister(2));

            node.shutdown();
        }


        @Test
        void testShutdown() {
            Map<String, Object> config = createBaseConfig();
            ModbusWriterNode node = new ModbusWriterNode("writer", config);

            node.initialize();
            assertTrue(node.isConnected());

            node.shutdown();
            assertFalse(node.isConnected());
            assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
        }
    }
}