package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Getter
@Slf4j
public class ApiResponse<T> {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final boolean success;
    private final T data;
    private final String error;

    private ApiResponse(boolean success, T data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    /**
     * 3. 핵심 유틸리티: HttpExchange 객체에 JSON 응답을 직접 쏴주는 메서드
     */
    public void send(HttpExchange exchange, int statusCode) {
        try {

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            
            // 객체를 JSON 문자열로 변환 후 바이트 배열로 추출
            String jsonResponse = mapper.writeValueAsString(this);
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IOException e) {
            log.error("[ApiServer] 응답 전송 중 오류 발생", e);
        } finally {
            exchange.close();
        }
    }
}