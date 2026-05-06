package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.core.impl.ErrorPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ErrorHandlingTest {

    static class DummyExceptionNode extends AbstractNode {
        public DummyExceptionNode(String id) {
            super(id);
        }
        @Override
        protected void onProcess(Message message) {
            if (message.hasKey("fail")) {
                throw new IllegalArgumentException("Test Error");
            }
        }
    }

    @Test
    @DisplayName("에러 발생 시 분기: 예외 발생 시 error 포트로 메시지가 전송되어야 한다")
    void testErrorBranching() {
        DummyExceptionNode node = new DummyExceptionNode("dummy-1");
        node.setFlowId("test-flow");

        OutputPort errorPort = node.getOutputPort("error");
        Connection mockConnection = mock(Connection.class);
        errorPort.connect(mockConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fail", true);

        assertDoesNotThrow(() -> node.process(new Message(payload)));

        verify(mockConnection, times(1)).deliver(any(Message.class));
    }

    @Test
    @DisplayName("에러 메시지 내용: 전달된 에러 메시지 안에 예외 정보와 원본 데이터가 있어야 한다")
    void testErrorMessageContent() {
        DummyExceptionNode node = new DummyExceptionNode("dummy-2");
        node.setFlowId("test-flow");

        OutputPort errorPort = node.getOutputPort("error");
        Connection mockConnection = mock(Connection.class);
        errorPort.connect(mockConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fail", true);
        Message originalMessage = new Message(payload);

        node.process(originalMessage);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mockConnection, times(1)).deliver(captor.capture());

        Message errorMessage = captor.getValue();
        assertTrue(errorMessage.hasKey("error"), "error 객체가 맵에 포함되어야 합니다.");
        assertTrue(errorMessage.hasKey("errorNodeId"), "에러 발생 노드 ID가 포함되어야 합니다.");
        assertEquals("dummy-2", errorMessage.get("errorNodeId"));
        assertTrue(errorMessage.hasKey("originalMessage"), "원본 메시지가 보존되어야 합니다.");
    }

    @Test
    @DisplayName("에러 포트 미연결: 연결된 핸들러가 없어도 시스템이 뻗지 않고 로그만 남겨야 한다")
    void testErrorPortNotConnected() {
        ErrorPort errorPort = new ErrorPort();
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", new RuntimeException("Test"));
        errorMap.put("errorNodeId", "test-node");

        assertDoesNotThrow(() -> errorPort.send(new Message(errorMap)));
    }

    @Test
    @DisplayName("정상 처리 시: 예외가 없으면 error 포트는 침묵해야 한다")
    void testNormalProcessing() {
        DummyExceptionNode node = new DummyExceptionNode("dummy-3");
        node.setFlowId("test-flow");

        OutputPort errorPort = node.getOutputPort("error");
        Connection mockConnection = mock(Connection.class);
        errorPort.connect(mockConnection);

        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);

        node.process(new Message(payload));

        verify(mockConnection, never()).deliver(any());
    }

    @Test
    @DisplayName("ErrorHandlerNode 수신: 에러 메시지를 정상적으로 처리하고 다음으로 넘겨야 한다")
    void testErrorHandlerNodeReceive() {
        ErrorHandlerNode handler = new ErrorHandlerNode("handler-1");
        handler.setFlowId("test-flow");

        OutputPort outPort = handler.getOutputPort("out");
        Connection mockConnection = mock(Connection.class);
        outPort.connect(mockConnection);

        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("error", new RuntimeException("Exception"));
        errorPayload.put("errorNodeId", "source-1");
        Message errorMessage = new Message(errorPayload);

        assertDoesNotThrow(() -> handler.process(errorMessage));

        verify(mockConnection, times(1)).deliver(any(Message.class));
    }

    @Test
    @DisplayName("DeadLetterNode: 들어온 메시지를 안전하게 버려야(또는 저장해야) 한다")
    void testDeadLetterNode() {
        DeadLetterNode dlq = new DeadLetterNode("dlq-1");
        dlq.setFlowId("test-flow");
        Message msg = new Message(new HashMap<>());

        assertDoesNotThrow(() -> dlq.process(msg));
    }
}