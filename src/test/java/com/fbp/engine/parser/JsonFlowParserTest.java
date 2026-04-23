package com.fbp.engine.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonFlowParserTest {

    JsonFlowParser parser;

    @BeforeEach
    void setUp() {
        parser = new JsonFlowParser();
    }

    InputStream toJsonStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("1. 정상 파싱 (All properties present)")
    void testNormalParsing() {
        String json = """
                {
                  "id": "temperature-monitoring",
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "nodes": [
                    {
                      "id": "sensor",
                      "type": "MqttSubscriber",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "sensor/temp",
                        "qos": 1
                      }
                    },
                    {
                      "id": "rule",
                      "type": "ThresholdFilter",
                      "config": {
                        "field": "value",
                        "operator": ">",
                        "threshold": 30
                      }
                    },
                    {
                      "id": "alert",
                      "type": "MqttPublisher",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "alert/temp"
                      }
                    }
                  ],
                  "connections": [
                    { "from": "sensor:out", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:in" }
                  ]
                }
                """;
        FlowDefinition flow = parser.parse(toJsonStream(json));

        assertEquals("temperature-monitoring", flow.id());
        assertEquals("온도 모니터링 플로우", flow.name());
        assertEquals("MQTT 센서 데이터를 수신하여 임계값 초과 시 알림", flow.description());
        assertEquals(3, flow.nodes().size());
        assertEquals(2, flow.connections().size());

        NodeDefinition node1 = flow.nodes().get(0);
        assertEquals("sensor", node1.id());
        assertEquals("MqttSubscriber", node1.type());
        assertEquals("tcp://localhost:1883", node1.config().get("broker"));
        assertEquals("sensor/temp", node1.config().get("topic"));
        assertEquals(1, node1.config().get("qos"));

        NodeDefinition node2 = flow.nodes().get(1);
        assertEquals("rule", node2.id());
        assertEquals("ThresholdFilter", node2.type());
        assertEquals("value", node2.config().get("field"));
        assertEquals(">", node2.config().get("operator"));
        assertEquals(30, node2.config().get("threshold"));

        NodeDefinition node3 = flow.nodes().get(2);
        assertEquals("alert", node3.id());
        assertEquals("MqttPublisher", node3.type());
        assertEquals("tcp://localhost:1883", node3.config().get("broker"));
        assertEquals("alert/temp", node3.config().get("topic"));

        ConnectionDefinition conn = flow.connections().get(0);
        assertEquals("sensor", conn.sourceId());
        assertEquals("out", conn.sourcePort());
        assertEquals("rule", conn.targetId());
        assertEquals("in", conn.targetPort());

        ConnectionDefinition conn1 = flow.connections().get(1);
        assertEquals("rule", conn1.sourceId());
        assertEquals("out", conn1.sourcePort());
        assertEquals("alert", conn1.targetId());
        assertEquals("in", conn1.targetPort());
    }

    @Test
    @DisplayName("필수 필드 누락 (id)")
    void testMissingId(){
        String json ="{\"nodes\":[]}";
        assertThrows(FlowParserException.class, ()-> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("3. nodes 누락/빈 배열")
    void testMissingNodes(){
        String json1 = "{\"id\": \"flow-1\"}";
        assertThrows(FlowParserException.class, ()-> parser.parse(toJsonStream(json1)));

        String json2="{ \"id\": \"flow-1\", \"nodes\": \"not-an-array\" }";
        assertThrows(FlowParserException.class, ()-> parser.parse(toJsonStream(json2)));
    }

    @Test
    @DisplayName("4. node의 필수 필드 누락 (id/type)")
    void testMissingNodeRequiredFields(){
        String jsonMissingType = """
            {
                "id": "flow-1",
                "nodes": [ { "id": "node1" } ]
            }
            """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(jsonMissingType)));

        String jsonMissingId = """
            {
                "id": "flow-1",
                "nodes": [ { "type": "TypeA" } ]
            }
            """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(jsonMissingId)));
    }
    @Test
    @DisplayName("5. connections 누락 시 빈 배열 반환")
    void testMissingConnections() {
        String json = """
            {
                "id": "flow-1",
                "nodes": [ { "id": "node1", "type": "TypeA" } ]
            }
            """;
        FlowDefinition flow = parser.parse(toJsonStream(json));
        assertNotNull(flow.connections());
        assertTrue(flow.connections().isEmpty());
    }

    @Test
    @DisplayName("6. connections의 from/to 누락")
    void testMissingFromToInConnections() {
        String json = """
            {
                "id": "flow-1",
                "nodes": [],
                "connections": [ { "from": "node1:out" } ]
            }
            """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("7. from/to 정규식 불일치 (포맷 에러)")
    void testInvalidConnectionFormat() {
        String json = """
            {
                "id": "flow-1",
                "nodes": [],
                "connections": [ { "from": "node1-out", "to": "node2:in" } ]
            }
            """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("8. name, description 누락 시 빈 문자열")
    void testMissingNameDescription() {
        String json = """
            {
                "id": "flow-1",
                "nodes": []
            }
            """;
        FlowDefinition flow = parser.parse(toJsonStream(json));
        assertEquals("", flow.name());
        assertEquals("", flow.description());
    }

    @Test
    @DisplayName("9. config가 없는 node 파싱")
    void testNodeWithoutConfig() {
        String json = """
            {
                "id": "flow-1",
                "nodes": [ { "id": "node1", "type": "TypeA" } ]
            }
            """;
        FlowDefinition flow = parser.parse(toJsonStream(json));
        NodeDefinition node = flow.nodes().get(0);
        assertNotNull(node.config());
        assertTrue(node.config().isEmpty());
    }

    @Test
    @DisplayName("10. config가 복잡한 중첩 객체일 때")
    @SuppressWarnings("unchecked")
    void testComplexNestedConfig() {
        String json = """
            {
                "id": "flow-1",
                "nodes": [
                    {
                        "id": "node1",
                        "type": "TypeA",
                        "config": {
                            "host": "localhost",
                            "port": 8080,
                            "nested": {
                                "active": true
                            }
                        }
                    }
                ]
            }
            """;
        FlowDefinition flow = parser.parse(toJsonStream(json));
        Map<String, Object> config = flow.nodes().get(0).config();

        assertEquals("localhost", config.get("host"));
        assertEquals(8080, config.get("port"));

        Map<String, Object> nested = (Map<String, Object>) config.get("nested");
        assertEquals(true, nested.get("active"));
    }

    @Test
    @DisplayName("11. 잘못된 JSON 문법")
    void testInvalidJsonSyntax() {
        String json = "{ \"id\": \"flow-1\" \"nodes\": [] }";
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("12. InputStream이 null이거나 닫힌 경우")
    void testNullOrClosedInputStream() throws IOException {
        assertThrows(FlowParserException.class, () -> parser.parse(null));

        InputStream errorStream = new InputStream() {
            @Override
            public int read() throws java.io.IOException {
                throw new java.io.IOException("Stream is closed");
            }
        };

        assertThrows(FlowParserException.class, () -> parser.parse(errorStream));
    }


}