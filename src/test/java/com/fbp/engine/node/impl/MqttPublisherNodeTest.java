package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.*;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MqttPublisherNodeTest {

    @Nested
    @DisplayName("단위 테스트")
    class UnitTest {
        private Map<String, Object> config;
        private MqttPublisherNode node;

        @BeforeEach
        void setUp() {
            config = new HashMap<>();
            config.put("brokerUrl", "tcp://localhost:1883");
            config.put("clientId", "test-unit-pub");
            config.put("topic", "default/topic");
            config.put("qos", 1);
            node = new MqttPublisherNode("pub_node", config);

        }

        @Test
        @DisplayName("입력 포트 구성 확인")
        void test1_InputPortNotNull() {
            assertNotNull(node.getInputPort("in"));
        }

        @Test
        @DisplayName("초기 연결 상태 확인")
        void test2_InitialStateIsDisconnected() {
            assertFalse(node.isConnected());
        }

        @Test
        @DisplayName("기본 토픽 설정값 조회")
        void test3_ConfigTopicRetrieval() {
            assertEquals("default/topic", node.getConfig("topic"));
        }

    }

    @Nested
    @Tag("integration")
    @TestInstance(Lifecycle.PER_CLASS)
    @DisplayName("통합 테스트")
    class IntegrationTest{
        private FlowEngine engine;
        private MqttPublisherNode pubNode;
        private DummySourceNode sourceNode;

        private MqttClient testSubscriberClient;
        private final Map<String, Object> receivedMessages = new ConcurrentHashMap<>();

        private final String BROKER_URL = "tcp://localhost:1883";
        private final String DEFAULT_TOPIC = "test/integration/default";
        private final String DYNAMIC_TOPIC = "test/integration.dynamic";

        class DummySourceNode extends AbstractNode{
            public DummySourceNode(String id) {
                super(id);
                addOutputPort("out");
            }

            @Override
            protected void onProcess(Message message) {

            }
            public void trigger(Message message){
                send("out", message);
            }
        }

        @BeforeAll
        void setupSubscriber() throws Exception{
            testSubscriberClient = new MqttClient(BROKER_URL, "test-integration-subscriber");
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setCleanStart(true);

            testSubscriberClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    receivedMessages.put(topic, new String(message.getPayload()));
                }
                @Override public void disconnected(MqttDisconnectResponse response) {}
                @Override public void mqttErrorOccurred(MqttException exception) {}
                @Override public void deliveryComplete(IMqttToken token) {}
                @Override public void connectComplete(boolean reconnect, String serverURI) {}
                @Override public void authPacketArrived(int reasonCode, MqttProperties properties) {}
            });
            testSubscriberClient.connect(options);
            testSubscriberClient.subscribe(DEFAULT_TOPIC, 1);
            testSubscriberClient.subscribe(DYNAMIC_TOPIC, 1);
        }

        @AfterAll
        void teardownSubscriber() throws Exception{
            if(testSubscriberClient != null && testSubscriberClient.isConnected()){
                testSubscriberClient.disconnect();
                testSubscriberClient.close();
            }
        }

        @BeforeEach
        void setUpFlow(){
            receivedMessages.clear();
            engine = new FlowEngine();
            Flow flow = new Flow("pub-integration-flow");

            Map<String, Object> config = new HashMap<>();
            config.put("brokerUrl", BROKER_URL);
            config.put("clientId", "test-integration-pub-node");
            config.put("topic", DEFAULT_TOPIC);
            config.put("qos", 1);

            pubNode = new MqttPublisherNode("pub_node", config);
            sourceNode = new DummySourceNode("source_node");

            flow.addNode(sourceNode)
                    .addNode(pubNode)
                    .connect(sourceNode.getId(), "out", pubNode.getId(), "in");
            engine.register(flow);
        }

        @Test
        @DisplayName("Broker 연결 성공 확인")
        void test4_BrokerConnectionSuccess() throws InterruptedException{
            engine.startFlow("pub-integration-flow");
            Thread.sleep(500);

            assertTrue(pubNode.isConnected());

            engine.shutdown();
        }

        @Test
        @DisplayName("메시지 기본 토픽 발행 확인")
        void test5_MessagePublishing() throws Exception{
            engine.startFlow("pub-integration-flow");
            Thread.sleep(1000);

            Map<String, Object> payload = new HashMap<>();
            payload.put("data", "hello default");
            sourceNode.trigger(new Message(payload));

            Thread.sleep(1000);

            assertTrue(receivedMessages.containsKey(DEFAULT_TOPIC));
            engine.shutdown();
        }

        @Test
        @DisplayName("동적 토픽 발행 확인")
        void test6_DynamicTopicPublishing() throws Exception{
            engine.startFlow("pub-integration-flow");
            Thread.sleep(1000);

            Map<String, Object> payload = new HashMap<>();
            payload.put("data", "hello dynamic");
            payload.put("topic", DEFAULT_TOPIC);
            sourceNode.trigger(new Message(payload));
            Thread.sleep(1000);

            assertTrue(receivedMessages.containsKey(DEFAULT_TOPIC));

            engine.shutdown();
        }

        @Test
        @DisplayName("Shutdown 시 연결 해제 확인")
        void test7_ShutdownDisconnects() throws InterruptedException{
            engine.startFlow("pub-integration-flow");
            Thread.sleep(500);
            assertTrue(pubNode.isConnected());

            engine.shutdown();
            Thread.sleep(500);

            assertFalse(pubNode.isConnected());
        }
    }
}