package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.MqttPublisherNode;
import com.fbp.engine.node.impl.MqttSubscriberNode;
import java.util.HashMap;
import java.util.Map;

public class MqttSubPubMain {
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String SUB_CLIENT_ID = "client-1";
    private static final String PUB_CLIENT_ID = "client-2";
    private static final String SUB_TOPIC = "sensor/temp";
    private static final String PUB_TOPIC = "alert/temp";
    private static final int QOS = 1;
    private static final boolean RETAINED = false;

    public static void main(String[] args) throws InterruptedException {
        Flow flow = new Flow("flow");
        FlowEngine engine = new FlowEngine();

        Map<String, Object> subConfig = new HashMap<>();
        subConfig.put("brokerUrl", BROKER_URL);
        subConfig.put("clientId", SUB_CLIENT_ID);
        subConfig.put("topic", SUB_TOPIC);
        subConfig.put("qos", QOS);

        Map<String, Object> pubConfig = new HashMap<>();
        pubConfig.put("brokerUrl", BROKER_URL);
        pubConfig.put("clientId", PUB_CLIENT_ID);
        pubConfig.put("topic", PUB_TOPIC);
        pubConfig.put("qos", QOS);
        pubConfig.put("retained", RETAINED);

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("sub_mqtt", subConfig);
        MqttPublisherNode publisherNode = new MqttPublisherNode("pub_mqtt", pubConfig);

        FilterNode filterNode = new FilterNode("filter", "temperature", 30);

        flow.addNode(subscriberNode)
                .addNode(publisherNode)
                .addNode(filterNode)
                .connect(subscriberNode.getId(), "out", filterNode.getId(), "in")
                .connect(filterNode.getId(),"out", publisherNode.getId(), "in");

        engine.register(flow);
        engine.startFlow(flow.getId());

        Thread.sleep(60000);

        engine.shutdown();
    }
}
