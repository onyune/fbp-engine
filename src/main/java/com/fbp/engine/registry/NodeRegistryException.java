package com.fbp.engine.registry;

/**
 * 레지스트리 관련 에러를 처리할 예외 클래스
 */
public class NodeRegistryException extends RuntimeException {
    public NodeRegistryException(String message) {
        super(message);
    }

    public NodeRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
