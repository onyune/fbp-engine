package com.fbp.engine.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.api.HttpApiServer;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.registry.NodeRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class RestApiIntegrationTest {

    private static HttpApiServer apiServer;
    private static HttpClient httpClient;
    private static final String BASE_URL = "http://localhost:8080";

    @BeforeAll
    static void setUp() throws Exception {
        NodeRegistry nodeRegistry = new NodeRegistry();
        nodeRegistry.register("PrintNode", config -> {
            return new com.fbp.engine.node.impl.PrintNode("test-print-node");
        });
        FlowEngine flowEngine = new FlowEngine();
        FlowManager flowManager = new FlowManager(nodeRegistry, flowEngine);

        apiServer = new HttpApiServer(8080);
        apiServer.start(flowManager);

        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (apiServer != null) {
            apiServer.stop();
        }
    }

    @Test
    @DisplayName("Health Check API Test")
    void testHealthCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("status"));
    }

    @Test
    @DisplayName("Deploy Flow API Test")
    void testDeployFlow() throws Exception {
        String flowJson = "{"
                + "\"id\":\"test-flow\","
                + "\"name\":\"Test Flow\","
                + "\"nodes\":[{\"id\":\"print-1\", \"type\":\"PrintNode\", \"config\":{}}],"
                + "\"connections\":[]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(flowJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("Get Flows API Test")
    void testGetFlows() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }
}