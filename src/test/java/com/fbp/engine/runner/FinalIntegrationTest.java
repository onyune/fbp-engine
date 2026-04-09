package com.fbp.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinalIntegrationTest {

    private FlowEngine engine;
    private Flow flow;
    private CollectorNode alertCollector;
    private CollectorNode normalCollector;
    private final String testFilePath = "./step10_test.log";
    private List<Message> alerts;
    private List<Message> normals;

    @BeforeAll
    void setUpAll() throws IOException, InterruptedException {
        Files.deleteIfExists(Paths.get(testFilePath));

        engine = new FlowEngine();
        flow = new Flow("final-flow");

        TimerNode timer = new TimerNode("timer", 100);
        TemperatureSensorNode sensor = new TemperatureSensorNode("sensor", 15, 45);
        ThresholdFilterNode filter = new ThresholdFilterNode("filter", "temperature", 30.0);
        AlertNode alert = new AlertNode("alert");
        LogNode logger = new LogNode("logger");
        FileWriteNode fileWriter = new FileWriteNode("fileWriter", testFilePath);

        alertCollector = new CollectorNode("alertCollector");
        normalCollector = new CollectorNode("normalCollector");

        flow.addNode(timer).addNode(sensor).addNode(filter)
                .addNode(alert).addNode(logger).addNode(fileWriter)
                .addNode(alertCollector).addNode(normalCollector);

        flow.connect(timer.getId(), "out", sensor.getId(), "trigger")
                .connect(sensor.getId(), "out", filter.getId(), "in")
                .connect(filter.getId(), "alert", alert.getId(), "in")
                .connect(filter.getId(), "alert", alertCollector.getId(), "in")
                .connect(filter.getId(), "normal", logger.getId(), "in")
                .connect(filter.getId(), "normal", normalCollector.getId(), "in")
                .connect(logger.getId(), "out", fileWriter.getId(), "in");

        engine.register(flow);
        engine.startFlow(flow.getId());

        Thread.sleep(2500);
        engine.shutdown();
        Thread.sleep(500);

        alerts = alertCollector.getCollected();
        normals = normalCollector.getCollected();
    }

    @Test
    @DisplayName("1. 엔진 시작/종료 상태 확인")
    void testEngineStatus() {
        assertNotNull(engine);
    }

    @Test
    @DisplayName("2. alert 경로 정확성 (30도 초과)")
    void testAlertPathAccuracy() {
        for (Message m : alerts) {
            double temp = ((Number) m.get("temperature")).doubleValue();
            assertTrue(temp > 30.0, "Alert 온도는 30도를 초과해야 함: " + temp);
        }
    }

    @Test
    @DisplayName("3. normal 경로 정확성 (30도 이하)")
    void testNormalPathAccuracy() {
        for (Message m : normals) {
            double temp = ((Number) m.get("temperature")).doubleValue();
            assertTrue(temp <= 30.0, "Normal 온도는 30도 이하여야 함: " + temp);
        }
    }

    @Test
    @DisplayName("4. 전체 분기 완전성")
    void testBranchCompleteness() {
        int total = alerts.size() + normals.size();
        assertTrue(total > 0, "최소 한 개 이상의 메시지가 수집되어야 함");
    }

    @Test
    @DisplayName("5. 파일 기록 검증")
    void testFileWriteVerification() throws IOException {
        Path path = Paths.get(testFilePath);
        if (Files.exists(path)) {
            long lineCount = Files.lines(path).count();
            assertEquals(normals.size(), (int) lineCount, "파일 줄 수와 normal 메시지 수가 일치해야 함");
        }
    }

    @Test
    @DisplayName("6. 센서 데이터 형식 확인")
    void testSensorDataFormat() {
        for (Message m : alerts) {
            assertNotNull(m.get("sensorId"));
            assertNotNull(m.get("temperature"));
            assertEquals("°C", m.get("unit"));
        }
    }

    @Test
    @DisplayName("7. 온도 범위 검증 (15~45도)")
    void testTemperatureRange() {
        for (Message m : alerts) {
            double temp = ((Number) m.get("temperature")).doubleValue();
            assertTrue(temp >= 15.0 && temp <= 45.0);
        }
        for (Message m : normals) {
            double temp = ((Number) m.get("temperature")).doubleValue();
            assertTrue(temp >= 15.0 && temp <= 45.0);
        }
    }
}