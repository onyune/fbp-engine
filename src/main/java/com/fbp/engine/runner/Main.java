package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

public class Main {
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Flow flow = new Flow("flowTest");

        TimerNode timerNode = new TimerNode("timer", 1000);
        LogNode logger = new LogNode("logger");
        FilterNode filter = new FilterNode("filter","tick", 3);
        PrintNode printer = new PrintNode("printer");

        flow.addNode(timerNode)
                .addNode(logger)
                .addNode(filter)
                .addNode(printer);

        flow.connect(timerNode.getId(), "out", logger.getId(), "in")
                .connect(logger.getId(), "out", filter.getId(), "in")
                .connect(filter.getId(), "out", printer.getId(), "in");

        Connection c1 = flow.getConnections().get(0);
        Connection c2 = flow.getConnections().get(1);
        Connection c3 = flow.getConnections().get(2);

        Thread t1 = new Thread(() -> { while (running) { Message m = c1.poll(); if (m != null) logger.getInputPort("in").receive(m); } });
        Thread t2 = new Thread(() -> { while (running) { Message m = c2.poll(); if (m != null) filter.getInputPort("in").receive(m); } });
        Thread t3 = new Thread(() -> { while (running) { Message m = c3.poll(); if (m != null) printer.getInputPort("in").receive(m); } });

        t1.start();
        t2.start();
        t3.start();
        flow.initialize();

        try{
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running=false;
        t1.interrupt();
        t2.interrupt();
        t3.interrupt();
        flow.shutdown();
    }
}