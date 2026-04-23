package com.fbp.engine.parser;

import java.util.Map;

public record NodeDefinition(
        String id,
        String type,
        Map<String, Object> config
) {

    public NodeDefinition{
        if(config != null){
            config=Map.copyOf(config);
        }else{
            config=Map.of();
        }
    }
}
