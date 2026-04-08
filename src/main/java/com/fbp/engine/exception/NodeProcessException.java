package com.fbp.engine.exception;

public class NodeProcessException extends RuntimeException {
    public NodeProcessException(String message) {
        super(message);
    }

    public NodeProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
