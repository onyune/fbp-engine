package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemperatureSensorNodeTest {
    TemperatureSensorNode sensor;
    Connection outConn;

    @BeforeEach
    void setUp(){
        sensor = new TemperatureSensorNode("temperature", 0, 50);
        outConn = new Connection("outConn");
        sensor.getOutputPort("out").connect(outConn);
    }

    @Test
    @DisplayName("온도 범위 확인")
    void checkTemperatureRage(){
        for(int i = 0 ; i<100;i++){
            sensor.onProcess(new Message(new HashMap<>()));

            Message outMsg = outConn.poll();
            assertNotNull(outMsg);

            double temperature = ((Number) outMsg.get("temperature")).doubleValue();

            assertTrue(temperature>=0.0 && temperature<=50.0);

        }
    }

    @Test
    @DisplayName("필수 키 포함")
    void containPrimaryKey(){

        sensor.onProcess(new Message(new HashMap<>()));
        Message outMsg = outConn.poll();

        assertNotNull(outMsg);
        assertTrue(outMsg.hasKey("sensorId"));
        assertTrue(outMsg.hasKey("temperature"));
        assertTrue(outMsg.hasKey("unit"));
        assertTrue(outMsg.hasKey("timestamp"));
    }
    @Test
    @DisplayName("sensorId 일치")
    void equalSensorIdAndNodeId(){

        sensor.onProcess(new Message(new HashMap<>()));
        Message outMsg = outConn.poll();

        String sensorId = outMsg.get("sensorId");
        String nodeId = "temperature";
        assertEquals(nodeId, sensorId);
    }
    @Test
    @DisplayName("트리거마다 생성")
    void createByTrigger(){


        sensor.onProcess(new Message(new HashMap<>()));
        sensor.onProcess(new Message(new HashMap<>()));
        sensor.onProcess(new Message(new HashMap<>()));

        assertNotNull(outConn.poll(), "1번째 메시지가 없습니다.");
        assertNotNull(outConn.poll(), "2번째 메시지가 없습니다.");
        assertNotNull(outConn.poll(), "3번째 메시지가 없습니다.");

        assertEquals(0, outConn.getBufferSize(), "예상보다 많은 메시지 생성됨");
    }
}