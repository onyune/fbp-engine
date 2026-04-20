package com.fbp.engine.runner;

import com.fbp.engine.node.impl.EchoProtocolNode;
import java.util.HashMap;
import java.util.Map;

public class EchoNodeMain {
    public static void main(String[] args) throws InterruptedException {
        Map<String, Object> config = new HashMap<>();
        config.put("host", "localhost");
        config.put("port", 9999);
        config.put("reconnectIntervalMs", 2000L);
        config.put("maxRetries", 10);

        EchoProtocolNode echoNode = new EchoProtocolNode("test-echo-node", config);

        echoNode.initialize();

        // 메인 스레드가 바로 종료되지 않도록 30초 대기
        // 이 시간 동안 서버를 껐다 켜보면서 재연결 동작을 눈으로 확인.
        System.out.println("=== 30초 대기 중... 서버를 종료했다가 다시 키기! ===");
        Thread.sleep(30000);

        // 엔진 종료 시 노드 자원 해제 테스트
        System.out.println("=== Echo Node 종료 ===");
        echoNode.shutdown();
    }
}
