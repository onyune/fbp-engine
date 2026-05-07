package com.fbp.engine.parser;

public record TransportDefinition(
        String type,
        String broker,
        Integer qos
) {
}
