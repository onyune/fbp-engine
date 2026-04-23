package com.fbp.engine.parser;

public record ConnectionDefinition(
        String sourceId,
        String sourcePort,
        String targetId,
        String targetPort
) {
}
