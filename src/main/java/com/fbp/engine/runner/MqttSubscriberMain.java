package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.impl.MqttSubscriberNode;
import com.fbp.engine.node.impl.PrintNode;
import java.util.HashMap;
import java.util.Map;

public class MqttSubscriberMain {

    public static void main(String[] args) throws InterruptedException {
        Flow flow = new Flow("flow");
        FlowEngine engine = new FlowEngine();
        String brokerUrl = "tcp://localhost:1883"; // Docker로 띄운 로컬 모스퀴토
        String clientId = "paho-test-client";      // 임의의 클라이언트 ID
        String testTopic = "sensor/test";          // 테스트용 토픽

        Map<String, Object> subConfig = new HashMap<>();
        subConfig.put("brokerUrl", brokerUrl);
        subConfig.put("clientId", clientId);
        subConfig.put("topic", testTopic);
        subConfig.put("qos", 1);

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("mqttSub", subConfig);

        PrintNode printNode = new PrintNode("printer");
        flow.addNode(subscriberNode)
                .addNode(printNode)
                .connect(subscriberNode.getId(), "out", printNode.getId(), "in");

        engine.register(flow);
        engine.startFlow(flow.getId());
        Thread.sleep(10000);

        engine.shutdown();

    }
}
