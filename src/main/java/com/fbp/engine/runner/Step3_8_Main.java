package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.impl.ModbusReaderNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Step3_8_Main {
    public static void main(String[] args) throws InterruptedException {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();

        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.setRegister(2,1);

        TimerNode timerNode = new TimerNode("time", 2000);

        Map<String, Object> readerConfig = new HashMap<>();
        readerConfig.put("host", "localhost");
        readerConfig.put("port", 5020);
        readerConfig.put("slaveId", 1);
        readerConfig.put("startAddress", 0);
        readerConfig.put("count", 3);

        Map<String, Object> mapping = new HashMap<>();
        mapping.put("0", Map.of("name", "temperature", "scale", 0.1));
        mapping.put("1", Map.of("name", "humidity", "scale", 0.1));
        mapping.put("2", Map.of("name", "status", "scale", 1.0));
        readerConfig.put("registerMapping", mapping);

        ModbusReaderNode readerNode= new ModbusReaderNode("modbus-reader", readerConfig);

        PrintNode printer = new PrintNode("printer");

        Flow flow = new Flow("flow");
        flow.addNode(timerNode)
                .addNode(readerNode)
                .addNode(printer)
                .connect(timerNode.getId(),"out", readerNode.getId(),"trigger")
                .connect(readerNode.getId(), "out", printer.getId(), "in");

        FlowEngine engine = new FlowEngine();
        engine.register(flow);
        engine.startFlow(flow.getId());

        Thread.sleep(5000);
        log.info("온도를 27.5도로 변경");
        simulator.setRegister(0, 275);
        Thread.sleep(5000);

        engine.shutdown();
        simulator.stop();

    }
}
