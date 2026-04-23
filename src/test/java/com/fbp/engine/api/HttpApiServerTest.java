package com.fbp.engine.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.registry.NodeRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HttpApiServerTest {

    private static final int TEST_PORT = 8080;
    private HttpApiServer apiServer;
    private FlowManager flowManager;
    private HttpClient client;

    private final String VALID_FLOW_JSON = """
            {
              "id": "%s",
              "name": "Test Flow",
              "nodes": [
                {
                  "id": "node-1",
                  "type": "DummyType",
                  "config": {}
                }
              ],
              "connections": []
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        NodeRegistry registry = new NodeRegistry();
        FlowEngine engine = new FlowEngine();
        flowManager = new FlowManager(registry, engine);

        // Mockito를 이용해 엔진의 instanceof AbstractNode 검사를 통과하는 가짜 노드 주입
        registry.register("DummyType", config -> {
            AbstractNode mockNode = Mockito.mock(AbstractNode.class);
            Mockito.when(mockNode.getId()).thenReturn("node-1");
            return mockNode;
        });

        apiServer = new HttpApiServer(TEST_PORT);
        apiServer.start(flowManager);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        apiServer.stop();
    }

    @Test
    @DisplayName("1. 서버 시작/정지: start() → 포트 바인딩 확인, stop() → 정상 종료")
    void testServerLifecycle() {
        assertDoesNotThrow(() -> {
            HttpClient testClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
                    .GET()
                    .build();
            testClient.send(request, HttpResponse.BodyHandlers.ofString());
        });
    }

    @Test
    @DisplayName("2. GET /health: 200 OK, status 필드 포함")
    void testHealthCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    @DisplayName("3. GET /flows: 200 OK, 배포된 플로우 목록 반환")
    void testListFlows() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"success\":true"));
    }

    @Test
    @DisplayName("4. POST /flows: 유효한 JSON → 201 Created, 플로우 배포 확인")
    void testDeployFlowSuccess() throws Exception {
        String json = String.format(VALID_FLOW_JSON, "test-flow");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("DEPLOYED"));
    }

    @Test
    @DisplayName("5. POST /flows: 잘못된 JSON → 400 Bad Request")
    void testDeployFlowInvalidJson() throws Exception {
        String invalidJson = "{ \"invalid\": json }";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("6. DELETE /flows/{id}: 존재하는 플로우 삭제 → 200 OK")
    void testDeleteFlowSuccess() throws Exception {
        String flowId = "delete-me";
        String json = String.format(VALID_FLOW_JSON, flowId);

        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows"))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows/" + flowId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("removed"));
    }

    @Test
    @DisplayName("7. DELETE /flows/{id}: 없는 id → 400 Bad Request (엔진 정책)")
    void testDeleteNonExistentFlow() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows/ghost-id"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("8. GET /flows/{id}/metrics: 배포된 플로우의 메트릭 JSON 반환")
    void testGetFlowMetrics() throws Exception {
        String flowId = "metrics-flow";
        String json = String.format(VALID_FLOW_JSON, flowId);

        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows"))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/flows/" + flowId + "/metrics"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"nodes\""));
    }

    @Test
    @DisplayName("9. 존재하지 않는 경로: 404 Not Found")
    void testNotFoundPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/invalid-path"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("10. 잘못된 HTTP 메서드: POST to /health → 405 Method Not Allowed")
    void testMethodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    @DisplayName("11. 포트 충돌: 이미 사용 중인 포트로 시작 시 예외")
    void testPortConflict() {
        HttpApiServer conflictServer = new HttpApiServer(TEST_PORT);
        assertThrows(IOException.class, () -> conflictServer.start(flowManager));
    }

    @Test
    @DisplayName("12. Content-Type: 응답 헤더에 application/json 포함")
    void testContentTypeHeader() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertTrue(response.headers().firstValue("Content-Type").isPresent());
        assertTrue(response.headers().firstValue("Content-Type").get().contains("application/json"));
    }
}