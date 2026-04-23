package com.fbp.engine.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonFlowParser implements FlowParser{
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern PORT_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+):([a-zA-Z0-9_-]+)$");

    @Override
    public FlowDefinition parse(InputStream inputStream) throws FlowParserException {
        try{
            JsonNode rootNode = mapper.readTree(inputStream);

            if(!rootNode.hasNonNull("id")){
                throw new FlowParserException("플로우 정의에 필수 필드 'id'가 누락되었습니다.");
            }

            if (!rootNode.hasNonNull("nodes") || !rootNode.get("nodes").isArray()){
                throw new FlowParserException("플로우 정의에 'nodes' 배열이 누락되었거나 형식이 일치하지않습니다.");
            }

            String flowId = rootNode.get("id").asText();
            String name = rootNode.hasNonNull("name")? rootNode.get("name").asText() : "";
            String description = rootNode.hasNonNull("description") ? rootNode.get("description").asText() : "";

            List<NodeDefinition> nodeDefs = new ArrayList<>();
            for(JsonNode node : rootNode.get("nodes")){
                if(!node.hasNonNull("id") || !node.hasNonNull("type")){
                    throw new FlowParserException("노드 정의에 필수 필드 'id' 또는 'type'이 누락되었습니다.");
                }
                String id = node.get("id").asText();
                String type = node.get("type").asText();

                Map<String, Object> config = Map.of();
                if(node.hasNonNull("config")){
                    config = mapper.convertValue(node.get("config"), Map.class);
                }

                nodeDefs.add(new NodeDefinition(id, type, config));
            }

            List<ConnectionDefinition> connDefs = new ArrayList<>();
            if(rootNode.hasNonNull("connections") && rootNode.get("connections").isArray()){
                for(JsonNode node : rootNode.get("connections")){
                    if(!node.hasNonNull("from") || !node.hasNonNull("to")){
                        throw new FlowParserException("연결 정의에 'from' 또는 'to' 필드가 누락되었습니다.");
                    }

                    String fromStr = node.get("from").asText();
                    String toStr = node.get("to").asText();

                    Matcher fromMatcher = PORT_PATTERN.matcher(fromStr);
                    Matcher toMatcher = PORT_PATTERN.matcher(toStr);

                    if(!fromMatcher.matches()){
                        throw new FlowParserException("잘못된 연결 형식입니다 (from): "+ fromStr);
                    }
                    if(!toMatcher.matches()){
                        throw new FlowParserException("잘못된 연결 형식입니다 (to): "+ toStr);
                    }

                    connDefs.add(new ConnectionDefinition(
                            fromMatcher.group(1),
                            fromMatcher.group(2),
                            toMatcher.group(1),
                            toMatcher.group(2)
                    ));
                }
            }
            return new FlowDefinition(flowId, name, description, nodeDefs, connDefs);
        }catch (FlowParserException e){
            throw e;
        } catch (Exception e) {
            throw new FlowParserException("JSON 플로우 정의 파싱 중 오류 발생: "+ e.getMessage());
        }
    }
}
