package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MqttSubscriberNodeTest {

    @Nested
    @DisplayName("단위 테스트")
    class UnitTest {

        private Map<String, Object> config;

        @Captor
        private ArgumentCaptor<MqttCallback> callbackCaptor;

        @BeforeEach
        void setUp() {
            config = new HashMap<>();
            config.put("brokerUrl", "tcp://localhost:1883");
            config.put("clientId", "test-unit-sub");
            config.put("topic", "sensor/unit");
            config.put("qos", 1);
        }

        @Test
        @DisplayName("포트 구성 확인")
        void test1_OutputPortNotNull() {
            MqttSubscriberNode node = new MqttSubscriberNode("sub_node", config);
            assertNotNull(node.getOutputPort("out"));
        }

        @Test
        @DisplayName("초기 연결 상태 확인")
        void test2_InitialStateIsDisconnected() {
            MqttSubscriberNode node = new MqttSubscriberNode("sub_node", config);
            assertFalse(node.isConnected());
        }

        @Test
        @DisplayName("설정값 조회 확인")
        void test3_ConfigRetrieval() {
            MqttSubscriberNode node = new MqttSubscriberNode("sub_node", config);
            assertEquals("tcp://localhost:1883", node.getConfig("brokerUrl"));
            assertEquals("test-unit-sub", node.getConfig("clientId"));
        }

        @Test
        @DisplayName("JSON 파싱 및 메시지 변환 성공")
        void test4_JsonToMessageConversion() throws Exception {
            List<Message> capturedMessages = new ArrayList<>();
            try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class)) {
                MqttSubscriberNode node = new MqttSubscriberNode("sub_node", config) {
                    @Override
                    protected void send(String portName, Message message) {
                        capturedMessages.add(message);
                    }
                };
                node.initialize();

                MqttClient mockClient = mocked.constructed().get(0);
                verify(mockClient).setCallback(callbackCaptor.capture());
                MqttCallback capturedCallback = callbackCaptor.getValue();

                String validJson = "{\"temperature\": 25.5, \"humidity\": 60}";
                capturedCallback.messageArrived("sensor/unit", new MqttMessage(validJson.getBytes()));

                assertFalse(capturedMessages.isEmpty());
                Map<String, Object> payload = capturedMessages.get(0).getPayload();

                assertEquals(25.5, ((Number) payload.get("temperature")).doubleValue());
                assertEquals("sensor/unit", payload.get("topic"));
            }
        }

        @Test
        @DisplayName("JSON 파싱 실패 시 원본 저장")
        void test5_InvalidJsonHandling() throws Exception {
            List<Message> capturedMessages = new ArrayList<>();
            try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class)) {
                MqttSubscriberNode node = new MqttSubscriberNode("sub_node", config) {
                    @Override
                    protected void send(String portName, Message message) {
                        capturedMessages.add(message);
                    }
                };
                node.initialize();

                MqttClient mockClient = mocked.constructed().get(0);
                verify(mockClient).setCallback(callbackCaptor.capture());
                MqttCallback capturedCallback = callbackCaptor.getValue();

                String invalidJson = "This is a plain text, not JSON";
                capturedCallback.messageArrived("sensor/unit", new MqttMessage(invalidJson.getBytes()));

                assertFalse(capturedMessages.isEmpty());
                Map<String, Object> payload = capturedMessages.get(0).getPayload();

                assertEquals(invalidJson, payload.get("rawPayload"));
            }
        }
    }

    @Nested
    @Tag("integration")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("통합 테스트")
    class IntegrationTest {

        private FlowEngine engine;
        private MqttSubscriberNode subNode;
        private CollectorNode collectorNode;

        private MqttClient testPublisherClient;
        private final String BROKER_URL = "tcp://localhost:1883";
        private final String TEST_TOPIC = "test/integration/sub";

        @BeforeAll
        void setupPublisher() throws Exception {
            testPublisherClient = new MqttClient(BROKER_URL, "test-integration-publisher");
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setCleanStart(true);
            testPublisherClient.connect(options);
        }

        @AfterAll
        void teardownPublisher() throws Exception {
            if (testPublisherClient != null && testPublisherClient.isConnected()) {
                testPublisherClient.disconnect();
                testPublisherClient.close();
            }
        }

        @BeforeEach
        void setUpFlow() {
            engine = new FlowEngine();
            Flow flow = new Flow("integration-flow");

            Map<String, Object> config = new HashMap<>();
            config.put("brokerUrl", BROKER_URL);
            config.put("clientId", "test-integration-sub-node");
            config.put("topic", TEST_TOPIC);
            config.put("qos", 1);

            subNode = new MqttSubscriberNode("sub_node", config);
            collectorNode = new CollectorNode("collector");

            flow.addNode(subNode)
                    .addNode(collectorNode)
                    .connect(subNode.getId(), "out", collectorNode.getId(), "in");

            engine.register(flow);
        }

        @Test
        @DisplayName("Broker 연결 성공 확인")
        void test6_BrokerConnectionSuccess() throws InterruptedException {
            engine.startFlow("integration-flow");
            Thread.sleep(500);

            assertTrue(subNode.isConnected());

            engine.shutdown();
        }

        @Test
        @DisplayName("메시지 수신 확인")
        void test7_MessageReception() throws Exception {
            engine.startFlow("integration-flow");
            Thread.sleep(1000);

            testPublisherClient.publish(TEST_TOPIC, new MqttMessage("{\"value\": 99}".getBytes()));
            Thread.sleep(1000);

            assertFalse(collectorNode.getCollected().isEmpty());

            engine.shutdown();
        }

        @Test
        @DisplayName("토픽 정보 포함 확인")
        void test8_TopicInfoIncluded() throws Exception {
            engine.startFlow("integration-flow");
            Thread.sleep(1000);

            testPublisherClient.publish(TEST_TOPIC, new MqttMessage("{\"value\": 99}".getBytes()));
            Thread.sleep(1000);

            Message received = collectorNode.getCollected().get(0);
            assertEquals(TEST_TOPIC, received.getPayload().get("topic"));

            engine.shutdown();
        }

        @Test
        @DisplayName("Shutdown 시 연결 해제 확인")
        void test9_ShutdownDisconnects() throws InterruptedException {
            engine.startFlow("integration-flow");
            Thread.sleep(500);
            assertTrue(subNode.isConnected());

            engine.shutdown();
            Thread.sleep(500);

            assertFalse(subNode.isConnected());
        }
    }
}