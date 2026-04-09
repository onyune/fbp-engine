package com.fbp.engine.exception;

public class NotFoundConfigKey extends RuntimeException {
    public NotFoundConfigKey(String key) {
        super("config Map에서 해당 key를 찾을 수 없습니다 : "+ key);
    }
}
