package com.fbp.engine.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProtocolNodeTest {
    static class DummyProtocolNode extends ProtocolNode {
        boolean shouldFail = false; // 테스트 목적: 강제 에러 유발 플래그
        int connectCallCount = 0;   // connect() 호출 횟수 추적
        int disconnectCallCount = 0;

        public DummyProtocolNode(String id, Map<String, Object> config) {
            super(id, config);
        }

        @Override
        protected void connect() throws Exception {
            connectCallCount++;
            if (shouldFail) {
                throw new Exception("테스트를 위한 강제 연결 실패");
            }
        }

        @Override
        protected void disconnect() throws Exception {
            disconnectCallCount++;
        }

        @Override
        protected void onProcess(Message message) {

        }
    }

    Map<String, Object> config;
    DummyProtocolNode node;

    @BeforeEach
    void setUp() {
        config = new HashMap<>();
        config.put("host", "localhost");
        config.put("port", 1883);
        config.put("reconnectIntervalMs", 100L);
        config.put("maxRetries", 3);
        node = new DummyProtocolNode("test-node", config);

    }

    @Test
    @DisplayName("초기 상태")
    void testInitialStateIsDisconnected(){
        assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
    }

    @Test
    @DisplayName("config 조회")
    void testSelectConfig(){
        assertEquals("localhost", node.getConfig("host"));
        assertEquals(1883, node.getConfig("port"));
        assertEquals(100L, node.getConfig("reconnectIntervalMs"));
        assertEquals(3, node.getConfig("maxRetries"));
    }

    @Test
    @DisplayName("initialize → CONNECTED")
    void testInitialize_Connected() throws Exception{

        node.shouldFail=false;
        node.initialize();

        assertEquals(ConnectionState.CONNECTED, node.getConnectionState());
        assertEquals(1, node.connectCallCount);
    }

    @Test
    @DisplayName("initialize → 연결 실패 시 상태")
    void testInitialize_fail(){
        node.shouldFail=true;
        node.initialize();
        assertEquals(ConnectionState.ERROR, node.getConnectionState());
    }

    @Test
    @DisplayName("shutdown → DISCONNECTED")
    void testShutDown_Disconnected(){
        node.initialize();
        node.shutdown();

        assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
    }

    @Test
    @DisplayName("isConnected 반환값")
    void returnIsConnected(){
        assertFalse(node.isConnected());

        node.initialize();
        assertTrue(node.isConnected());

        node.shutdown();
        assertFalse(node.isConnected());
    }
    @Test
    @DisplayName("재연결 시도")
    void retryConnect() throws InterruptedException{
        node.shouldFail=true;
        node.initialize();

        assertEquals(1, node.connectCallCount);

        Thread.sleep(250);

        assertTrue(node.connectCallCount>1);
        node.shutdown();
    }
}