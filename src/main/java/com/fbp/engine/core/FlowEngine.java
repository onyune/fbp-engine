package com.fbp.engine.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class FlowEngine {

    private enum State{
        INITIALIZED,
        RUNNING,
        STOPPED
    }

    private final Map<String, Flow> flows;
    private State state;

    public FlowEngine() {
        this.flows = new HashMap<>();
        this.state = State.INITIALIZED;
    }

    /**
     * 플로우를 flows 맵에 등록
     * @param flow
     */
    public void register(Flow flow){
        if(Objects.isNull(flow)){
            throw new IllegalArgumentException("flow is null");
        }
        flows.put(flow.getId(),flow);
        log.info("[Engine] 플로우 '{}' 등록됨",flow.getId());
    }

    /**
     * flow.initialize() 호출
     * @param flowId
     */
    public void startFlow(String flowId){
        Flow flow = flows.get(flowId);
        if(Objects.isNull(flow)){
            throw new IllegalArgumentException("flow가 null 입니다 flowId: "+ flowId);
        }
        List<String> errors = flow.validate(); // 순환참조도 검사
        if(!errors.isEmpty()){
            throw new IllegalStateException("flow에 문제가 발생했습니다. "+ errors.getFirst());
        }
        flow.initialize();

        this.state=State.RUNNING;

        log.info("[Engine] 플로우 '{}' 시작됨", flowId);
    }

    /**
     * 해당 플로우의 shutdown()호출
     * @param flowId
     */
    public void stopFlow(String flowId){
        Flow flow = flows.get(flowId);
        if(Objects.isNull(flow)){
            throw new IllegalArgumentException("flow가 null 입니다 flowId: "+ flowId);
        }
        flow.shutdown();
        log.info("[Engine] 플로우 '{}' 정지됨", flowId);
    }

    /**
     * 모든 플로우를 shutdown
     * state -> STOPPED
     */
    public void shutdown(){
        for(Flow f : flows.values()){
            f.shutdown();
        }
        state=State.STOPPED;
    }

}
