package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

public class CLIMain {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        FlowEngine engine = new FlowEngine();

        Flow flowA = new Flow("flowA");
        TimerNode timerA = new TimerNode("timerA", 1000);
        PrintNode printerA = new PrintNode("printerA");
        flowA.addNode(timerA).addNode(printerA)
                .connect(timerA.getId(), "out", printerA.getId(), "in");

        Flow flowB = new Flow("flowB");
        TimerNode timerB = new TimerNode("timerB", 2000);
        PrintNode printerB = new PrintNode("printerB");
        flowB.addNode(timerB).addNode(printerB)
                .connect(timerB.getId(), "out", printerB.getId(), "in");

        engine.register(flowA);
        engine.register(flowB);

        Connection cA = flowA.getConnections().get(0);
        Connection cB = flowB.getConnections().get(0);

        Thread workerA = new Thread(() -> {
            while (running) {
                Message m = cA.poll();
                if (m != null) printerA.getInputPort("in").receive(m);
            }
        });
        Thread workerB = new Thread(() -> {
            while (running) {
                Message m = cB.poll();
                if (m != null) printerB.getInputPort("in").receive(m);
            }
        });

        workerA.start();
        workerB.start();

        engine.startCLI();

        running = false;
        workerA.interrupt();
        workerB.interrupt();

    }
}
