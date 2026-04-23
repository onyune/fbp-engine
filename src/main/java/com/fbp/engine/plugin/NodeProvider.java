package com.fbp.engine.plugin;

import java.util.List;

/**
 * 1. 규격 정의
 * 이 인터페이스를 통해 메인 엔진은 "어떤 노드들을 제공할 것인가?" 라는 질문을 던지고
 * 플러그인은 NodeDescriptor 객체 리스트로 답함
 */
public interface NodeProvider {
    /**
     * 이 플러그인이 제공하는 노드 타입 목록
     */
    List<NodeDescriptor> getNodeDescriptors();
}
