package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.RoutingRule;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DynamicRouterNodeTest {

    private DynamicRouterNode routerNode;

    @BeforeEach
    void setUp() {
        routerNode = new DynamicRouterNode("router-1");
        routerNode.setFlowId("flow-1");
    }

    @Test
    @DisplayName("조건 매칭: 조건에 맞는 포트로 메시지가 전달되어야 한다")
    void testConditionMatching() {

        routerNode.addRule(new RoutingRule("type", "==", "temp", "tempPort"));

        OutputPort tempPort = routerNode.getOutputPort("tempPort");
        assertNotNull(tempPort, "addRule 호출 시 타겟 포트가 자동 생성되어야 합니다.");

        Connection mockConnection = mock(Connection.class);
        tempPort.connect(mockConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "temp");
        Message message = new Message(payload);

        routerNode.process(message);
        verify(mockConnection, times(1)).deliver(message);
    }

    @Test
    @DisplayName("다중 규칙: 여러 규칙 중 처음 매칭된 룰의 포트로만 전송되어야 한다")
    void testMultipleRules() {
        routerNode.addRule(new RoutingRule("val", ">", 50, "highPort"));
        routerNode.addRule(new RoutingRule("val", ">", 20, "midPort"));

        Connection mockMidConnection = mock(Connection.class);
        routerNode.getOutputPort("midPort").connect(mockMidConnection);

        Connection mockHighConnection = mock(Connection.class);
        routerNode.getOutputPort("highPort").connect(mockHighConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("val", 30);
        Message message = new Message(payload);

        routerNode.process(message);

        verify(mockMidConnection, times(1)).deliver(message);
        verify(mockHighConnection, never()).deliver(any());
    }

    @Test
    @DisplayName("기본 포트: 매칭되는 룰이 없으면 default 포트로 전송되어야 한다")
    void testDefaultPort() {
        routerNode.addRule(new RoutingRule("type", "==", "temp", "tempPort"));

        Connection mockDefaultConnection = mock(Connection.class);
        routerNode.getOutputPort("default").connect(mockDefaultConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "humidity");
        Message message = new Message(payload);

        routerNode.process(message);

        verify(mockDefaultConnection, times(1)).deliver(message);
    }

    @Test
    @DisplayName("규칙 없음: 룰이 하나도 없으면 무조건 default 포트로 전송되어야 한다")
    void testNoRules() {
        Connection mockDefaultConnection = mock(Connection.class);
        routerNode.getOutputPort("default").connect(mockDefaultConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("any", "value");
        Message message = new Message(payload);

        routerNode.process(message);

        verify(mockDefaultConnection, times(1)).deliver(message);
    }

    @Test
    @DisplayName("null 필드: 메시지에 검사할 키 값이 아예 없으면 default 포트로 전송되어야 한다")
    void testNullField() {
        routerNode.addRule(new RoutingRule("missing", "==", "val", "targetPort"));

        Connection mockDefaultConnection = mock(Connection.class);
        routerNode.getOutputPort("default").connect(mockDefaultConnection);

        Map<String, Object> payload = new HashMap<>();
        Message message = new Message(payload);

        routerNode.process(message);

        verify(mockDefaultConnection, times(1)).deliver(message);
    }

    @Test
    @DisplayName("런타임 규칙 변경: 규칙 추가 시 포트가 동적으로 계속 생성되어야 한다")
    void testRuntimeRuleChange() {
        routerNode.addRule(new RoutingRule("a", "==", 1, "port1"));
        assertNotNull(routerNode.getOutputPort("port1"));

        routerNode.addRule(new RoutingRule("b", "==", 2, "port2"));
        assertNotNull(routerNode.getOutputPort("port2"));
    }

    @Test
    @DisplayName("성능: 100개의 라우팅 룰을 순차 검사해도 50ms 안에 처리되어야 한다")
    void testPerformance() {
        for (int i = 0; i < 100; i++) {
            routerNode.addRule(new RoutingRule("key", "==", i, "port" + i));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("key", 99);
        Message message = new Message(payload);

        long start = System.currentTimeMillis();
        routerNode.process(message);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 50, "라우팅 처리가 50ms를 초과했습니다.");
        assertNotNull(routerNode.getOutputPort("port99"));
    }
}