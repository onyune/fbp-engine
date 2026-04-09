package com.fbp.engine.runner;


import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.impl.AlertNode;
import com.fbp.engine.node.impl.FileWriteNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.TemperatureSensorNode;
import com.fbp.engine.node.impl.ThresholdFilterNode;
import com.fbp.engine.node.impl.TimerNode;

public class Step10_Main {
    public static void main(String[] args) throws InterruptedException {
        Flow flow = new Flow("flow");
        FlowEngine flowEngine = new FlowEngine();

        TimerNode timerNode = new TimerNode("timer",10000);
        TemperatureSensorNode sensorNode = new TemperatureSensorNode("temperature", 15, 45);
        ThresholdFilterNode filterNode = new ThresholdFilterNode("filter", "temperature", 30);
        AlertNode alertNode = new AlertNode("alert");
        LogNode logNode = new LogNode("logger");
        FileWriteNode fileWriteNode = new FileWriteNode("file", "./step10");

        flow.addNode(timerNode)
                .addNode(sensorNode)
                .addNode(filterNode)
                .addNode(alertNode)
                .addNode(logNode)
                .addNode(fileWriteNode)
                .connect(timerNode.getId(), "out", sensorNode.getId(), "trigger")
                .connect(sensorNode.getId(), "out", filterNode.getId(), "in")
                .connect(filterNode.getId(), "alert", alertNode.getId(), "in")
                .connect(filterNode.getId(), "normal", logNode.getId(), "in")
                .connect(logNode.getId(), "out", fileWriteNode.getId(), "in");

        flowEngine.register(flow);
        flowEngine.startFlow(flow.getId());

        Thread.sleep(10000);

        flowEngine.shutdown();
    }



}
