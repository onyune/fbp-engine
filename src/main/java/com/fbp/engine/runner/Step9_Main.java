package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.impl.AlertNode;
import com.fbp.engine.node.impl.FileWriteNode;
import com.fbp.engine.node.impl.HumiditySensorNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.TemperatureSensorNode;
import com.fbp.engine.node.impl.ThresholdFilterNode;
import com.fbp.engine.node.impl.TimerNode;

public class Step9_Main {
    public static void main(String[] args) throws InterruptedException {
        Flow flow = new Flow("flow");
        FlowEngine flowEngine = new FlowEngine();

        TimerNode timer = new TimerNode("timer", 1000);

        TemperatureSensorNode temperatureSensorNode = new TemperatureSensorNode("temperatureSensorNode", 15, 45);
        HumiditySensorNode humiditySensorNode = new HumiditySensorNode("humiditySensorNode", 0, 100);

        ThresholdFilterNode filter1 = new ThresholdFilterNode("filter1","temperature", 30);
        ThresholdFilterNode filter2 = new ThresholdFilterNode("filter2","humidity", 69);

        AlertNode alert = new AlertNode("alert");
        LogNode logger = new LogNode("logger");

        FileWriteNode writeNode = new FileWriteNode("fileWrite", "./normalTemperature");



        flow.addNode(timer)
                .addNode(temperatureSensorNode)
                .addNode(humiditySensorNode)
                .addNode(filter1)
                .addNode(filter2)
                .addNode(alert)
                .addNode(logger)
                .addNode(writeNode)
                .connect(timer.getId(), "out", temperatureSensorNode.getId(), "trigger")
                .connect(timer.getId(), "out", humiditySensorNode.getId(), "trigger")
                .connect(temperatureSensorNode.getId(), "out", filter1.getId(), "in")
                .connect(humiditySensorNode.getId(), "out", filter2.getId(), "in")
                .connect(filter1.getId(), "alert", alert.getId(), "in")
                .connect(filter1.getId(), "normal", logger.getId(), "in")
                .connect(filter1.getId(), "normal", writeNode.getId(),"in")
                .connect(filter2.getId(), "alert", alert.getId(), "in");

        flowEngine.register(flow);
        flowEngine.startFlow(flow.getId());

        Thread.sleep(10000);

        flowEngine.shutdown();

    }
}
