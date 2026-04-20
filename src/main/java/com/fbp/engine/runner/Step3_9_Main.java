package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.ModbusReaderNode;
import com.fbp.engine.node.impl.ModbusWriterNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.ThresholdFilterNode;
import com.fbp.engine.node.impl.TimerNode;
import com.fbp.engine.node.impl.TransformNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Step3_9_Main {

    public static void main(String[] args) throws InterruptedException {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();

        simulator.setRegister(0, 250);
        simulator.setRegister(2, 0);

        TimerNode timerNode = new TimerNode("timer", 2000);

        Map<String, Object> readerConfig = new HashMap<>();
        readerConfig.put("host", "localhost");
        readerConfig.put("port", 5020);
        readerConfig.put("slaveId", 1);
        readerConfig.put("startAddress", 0);
        readerConfig.put("count", 1);
        readerConfig.put("registerMapping", Map.of("0", Map.of("name", "temperature", "scale", 0.1)));
        ModbusReaderNode reader = new ModbusReaderNode("reader", readerConfig);

        ThresholdFilterNode fileter = new ThresholdFilterNode("filter", "temperature", 30.0);

        TransformNode transformNode = new TransformNode("transform", msg -> {
            Map<String, Object> payload = new HashMap<>(msg.getPayload());
            payload.put("alertCode", 1);
            return new Message(payload);
        });

        Map<String, Object> writerConfig = new HashMap<>();
        writerConfig.put("host", "localhost");
        writerConfig.put("port", 5020);
        writerConfig.put("slaveId", 1);
        writerConfig.put("registerAddress", 2);
        writerConfig.put("valueField", "alertCode");
        ModbusWriterNode writer = new ModbusWriterNode("writer", writerConfig);
        PrintNode printer = new PrintNode("printer");

        Flow flow = new Flow("flow");
        flow.addNode(timerNode)
                .addNode(reader)
                .addNode(fileter)
                .addNode(transformNode)
                .addNode(writer)
                .addNode(printer)
                .connect(timerNode.getId(), "out", reader.getId(), "trigger")
                .connect(reader.getId(),"out", fileter.getId(), "in")
                .connect(fileter.getId(), "alert", transformNode.getId(), "in")
                .connect(transformNode.getId(), "out", writer.getId(), "in")
                .connect(writer.getId(), "result", printer.getId(), "in");

        FlowEngine engine = new FlowEngine();
        engine.register(flow);
        engine.startFlow(flow.getId());

        Thread.sleep(4000);
        log.info("온도 상승 32.5");
        simulator.setRegister(0, 325);
        Thread.sleep(4000);

        int finalAlertStatus = simulator.getRegister(2);
        if(finalAlertStatus==1){
            log.info("성공 ");
        }else{
            log.info("실패 알림 레지스터 변경 실패!!");
        }

        engine.shutdown();
        simulator.stop();
    }
}
