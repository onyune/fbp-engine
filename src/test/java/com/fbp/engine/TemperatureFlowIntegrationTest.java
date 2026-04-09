package com.fbp.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.CollectorNode;
import com.fbp.engine.node.impl.TemperatureSensorNode;
import com.fbp.engine.node.impl.ThresholdFilterNode;
import com.fbp.engine.node.impl.TimerNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemperatureFlowIntegrationTest {

    private CollectorNode alertCollector;
    private CollectorNode normalCollector;

    @BeforeEach
    void setUp() throws InterruptedException {
        Flow flow = new Flow("integration-flow");
        FlowEngine engine = new FlowEngine();

        TimerNode timer = new TimerNode("timer", 100);
        TemperatureSensorNode sensor = new TemperatureSensorNode("sensor", 15, 45);
        ThresholdFilterNode filter = new ThresholdFilterNode("filter", "temperature", 30.0);

        alertCollector = new CollectorNode("alertCollector");
        normalCollector = new CollectorNode("normalCollector");

        flow.addNode(timer)
                .addNode(sensor)
                .addNode(filter)
                .addNode(alertCollector)
                .addNode(normalCollector)
                .connect(timer.getId(), "out", sensor.getId(), "trigger")
                .connect(sensor.getId(), "out", filter.getId(), "in")
                .connect(filter.getId(), "alert", alertCollector.getId(), "in")
                .connect(filter.getId(), "normal", normalCollector.getId(), "in");

        engine.register(flow);

        engine.startFlow(flow.getId());
        Thread.sleep(2000);
        engine.shutdown();

        Thread.sleep(500);
    }

    @Test
    @DisplayName("alert 경로 검증")
    void testAlertPath() {
        List<Message> alerts = alertCollector.getCollected();

        for (Message msg : alerts) {
            double temp = ((Number) msg.get("temperature")).doubleValue();
            assertTrue(temp > 30.0);
        }
    }

    @Test
    @DisplayName("normal 경로 검증")
    void testNormalPath() {
        List<Message> normals = normalCollector.getCollected();

        for (Message msg : normals) {
            double temp = ((Number) msg.get("temperature")).doubleValue();
            assertTrue(temp <= 30.0);
        }
    }

    @Test
    @DisplayName("전체 메시지 수")
    void testTotalMessageCount() {
        int totalCollected = alertCollector.getCollected().size() + normalCollector.getCollected().size();

        assertTrue(totalCollected >= 15 && totalCollected <= 25, "총 수집된 메시지 수가 타이머 tick 수와 일치하지 않습니다.");

        System.out.println("통합 테스트 완료 총 처리된 온도 데이터: " + totalCollected + "건");
        System.out.println("   Alert(30도 초과): " + alertCollector.getCollected().size() + "건");
        System.out.println("   Normal(30도 이하): " + normalCollector.getCollected().size() + "건");
    }
}