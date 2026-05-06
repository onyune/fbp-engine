package com.fbp.engine.core;

import com.fbp.engine.core.Flow.FlowState;
import com.fbp.engine.engine.ThreadPoolConfig;
import com.fbp.engine.message.Message;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class FlowEngine {

    public enum State{
        INITIALIZED,
        RUNNING,
        STOPPED
    }

    private final Map<String, Flow> flows;
    private final ExecutorService executor;
    private State state;

    public FlowEngine() {
        ThreadPoolConfig config = new ThreadPoolConfig();
        this.executor = config.createExecutor();
        this.flows = new ConcurrentHashMap<>();
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

        for(Connection conn : flow.getConnections()){
            executor.submit(()->{
                while(flow.getState() == FlowState.RUNNING){
                    try{
                        Message m = conn.poll();
                        if (m != null)
                            conn.getTarget().receive(m);
                    }catch(Exception e){
                        Thread.currentThread().interrupt();
                        break;
                    }

                }
            });
        }

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
        executor.shutdownNow();
    }

    /**
     * 모든 플로우의 ID와 상태를 출력
     * 특정 플로우만 stop/start 하면 해당 플로우만 상태가 변경되는지 확인
     */
    public void listFlows(){
        for(Flow f : flows.values()){
            log.info("flowId: {} - state: {}", f.getId(), f.getState());
        }
    }

    /**
     * FlowEngine CLI
     */
    public void startCLI(){
        Scanner sc = new Scanner(System.in);
        boolean running=true;
        while(running){
            System.out.print("fbp> ");
            String input = sc.nextLine().trim();
            if(input.isEmpty()) continue;

            String[] cli = input.split("\\s+");

            String command = cli[0].toLowerCase();
            try {
                switch (command) {
                    case "list":
                        listFlows();
                        break;
                    case "start":
                        if (cli.length < 2) {
                            System.out.println("사용법: start <flowId>");
                        } else {
                            startFlow(cli[1]);
                        }
                        break;
                    case "stop":
                        if (cli.length < 2) {
                            System.out.println("사용법: stop <flowId>");
                        } else {
                            stopFlow(cli[1]);
                        }
                        break;
                    case "exit": {
                        System.out.println("[Engine] CLI 모드를 종료합니다.");
                        shutdown();
                        running = false;

                    }
                    break;
                    default:
                }
            }catch (Exception e){
                System.out.println("에러 발생: "+ e.getMessage());
            }
        }
    }
}
