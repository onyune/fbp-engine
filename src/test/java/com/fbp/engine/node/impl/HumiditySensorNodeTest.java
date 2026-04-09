package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HumiditySensorNodeTest {

    @Test
    @DisplayName("습도 범위 확인")
    void checkHumidity(){
        HumiditySensorNode sensor = new HumiditySensorNode("humidity", 30, 90);
        Connection connection = new Connection("outConn");
        sensor.getOutputPort("out").connect(connection);

        for(int i = 0 ; i<100;i++){
            sensor.onProcess(new Message(new HashMap<>()));

            Message outMsg = connection.poll();
            assertNotNull(outMsg);

            double humidity = ((Number) outMsg.get("humidity")).doubleValue();

            assertTrue(humidity>=30.0 && humidity<=90.0);

        }
    }

    @Test
    @DisplayName("필수 키 포함")
    void containPrimaryKey(){
        HumiditySensorNode sensor = new HumiditySensorNode("humidity", 30, 90);
        Connection connection = new Connection("outConn");
        sensor.getOutputPort("out").connect(connection);
        sensor.onProcess(new Message(new HashMap<>()));
        Message outMsg = connection.poll();

        assertNotNull(outMsg);
        assertTrue(outMsg.hasKey("sensorId"));
        assertTrue(outMsg.hasKey("humidity"));
        assertTrue(outMsg.hasKey("unit"));
    }

    @Test
    @DisplayName("sensorId 일치")
    void equalSensorIdAndNodeId(){
        HumiditySensorNode sensor = new HumiditySensorNode("humidity", 30, 90);
        Connection connection = new Connection("outConn");
        sensor.getOutputPort("out").connect(connection);
        sensor.onProcess(new Message(new HashMap<>()));
        Message outMsg = connection.poll();

        String sensorId = outMsg.get("sensorId");
        String nodeId = "humidity";
        assertEquals(nodeId, sensorId);
    }

    @Test
    @DisplayName("트리거마다 생성")
    void createByTrigger(){
        HumiditySensorNode sensor = new HumiditySensorNode("humidity", 30, 90);
        Connection connection = new Connection("outConn");
        sensor.getOutputPort("out").connect(connection);

        sensor.onProcess(new Message(new HashMap<>()));
        sensor.onProcess(new Message(new HashMap<>()));
        sensor.onProcess(new Message(new HashMap<>()));

        assertNotNull(connection.poll(), "1번째 메시지가 없습니다.");
        assertNotNull(connection.poll(), "2번째 메시지가 없습니다.");
        assertNotNull(connection.poll(), "3번째 메시지가 없습니다.");

        assertEquals(0, connection.getBufferSize(), "예상보다 많은 메시지 생성됨");
    }

}