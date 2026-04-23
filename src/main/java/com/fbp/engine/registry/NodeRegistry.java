package com.fbp.engine.registry;

import com.fbp.engine.node.Node;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    //동시성 문제 방지
    private final Map<String, NodeFactory> registry = new ConcurrentHashMap<>();

    /**
     * 노트 팩토리 등록 (기본동작은 중복 시 예외 발생이지만 덮어쓰기 메서드 제공)
     * 기본으로 덮어쓰기 금지!
     */
    public void register(String typeName, NodeFactory factory){
        register(typeName, factory, false);
    }

    /**
     * 노드 팩토리 등록. 덮어쓰기 여부 명시 가능
     */
    public void register(String typeName, NodeFactory factory, boolean overwrite){
        if(typeName==null || typeName.trim().isEmpty()){
            throw new NodeRegistryException("타입 이름은 비어있을 수 없습니다.");
        }

        if(factory==null){
            throw new NodeRegistryException("등록할 NodeFactory는 null일 수 없습니다.");
        }

        if(!overwrite && registry.containsKey(typeName)){
            //중복 시 예외 발생
            throw new NodeRegistryException("이미 등록된 노드 타입입니다: "+ typeName);
        }
        registry.put(typeName, factory);
    }

    /**
     * 등록된 팩토리를 사용하여 새로운 노드 인스턴스 생성
     */

    public Node create(String typeName, Map<String, Object> config){
        if(typeName == null || typeName.trim().isEmpty()){
            throw new NodeRegistryException("타입 이름은 비어있을 수 없습니다.");
        }

        NodeFactory factory = registry.get(typeName);

        if(factory==null){
            throw new NodeRegistryException("등록되지 않은 노드 타입입니다: "+ typeName);
        }

        try{
            return factory.create(config);
        }catch (Exception e){
            throw new NodeRegistryException("노드 생성 중 오류 발생 ("+ typeName+")",e);
        }
    }

    /**
     * 현재 등록된 모든 노드 타입의 이름 목록을 반환
     */
    public Set<String> getRegisteredTypes(){
        return Set.copyOf(registry.keySet());
    }

    /**
     * 특정 타입의 노드가 등록되어 있는 지 확인
     */
    public boolean isRegistered(String typeName){
        if(typeName == null || typeName.trim().isEmpty()){
            throw new NodeRegistryException("타입 이름은 비어있을 수 없습니다.");
        }
        return registry.containsKey(typeName);
    }
}
