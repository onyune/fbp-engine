package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

public class Main {
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Flow flow1 = new Flow("flowTest1");
        Flow flow2 = new Flow("flowTest2");

        FlowEngine flowEngine = new FlowEngine();

        TimerNode timerNode1 = new TimerNode("timer1", 500);
        TimerNode timerNode2 = new TimerNode("timer2", 1000);

        PrintNode printNode1 = new PrintNode("printer1");
        PrintNode printNode2 = new PrintNode("printer2");

        flow1.addNode(timerNode1)
                .addNode(printNode1)
                .connect(timerNode1.getId(),"out", printNode1.getId(), "in");
        flow2.addNode(timerNode2)
                .addNode(printNode2)
                .connect(timerNode2.getId(),"out", printNode2.getId(), "in");

        Connection c1 = flow1.getConnections().get(0);
        Connection c2 = flow2.getConnections().get(0);

        Thread worker1 = new Thread(() -> {
            while (running) {
                Message m = c1.poll();
                if (m != null)
                    printNode1.getInputPort("in").receive(m);
            }
        });
        Thread worker2 = new Thread(() -> {
            while (running) { Message m = c2.poll(); if (m != null) printNode2.getInputPort("in").receive(m); }
        });

        worker1.start();
        worker2.start();
        flowEngine.register(flow1);
        flowEngine.register(flow2);

        flowEngine.startFlow(flow1.getId());
        flowEngine.startFlow(flow2.getId());

        Thread.sleep(5000);

        running=false;
        worker1.interrupt();
        worker2.interrupt();
        flowEngine.shutdown();
    }
}