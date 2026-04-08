package com.fbp.engine.core;

import com.fbp.engine.node.AbstractNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

public class Flow {

    private enum VisitStat {
        UNVISITED, // 아직 방문안함
        VISITING, // 방문중
        VISITED, // 방문함
    }
    public enum FlowState{
        RUNNING,
        STOPPED
    }

    //Flow id
    @Getter
    private final String id;

    //개별 플로우의 상태
    @Getter
    private FlowState state;

    //등록된 노드들
    private final Map<String, AbstractNode> nodes = new HashMap<>();

    //생성된 연결
    @Getter
    private final List<Connection> connections = new ArrayList<>();

    public Flow(String id) {
        this.id = id;
        this.state = FlowState.STOPPED;
    }

    /**
     * 노드 등록
     *
     * @param node 등록할 노드
     * @return 메서드 체이닝 지원으로 this 호출
     * <p>
     * flow.addNode(new TimerNode("timer", 1000)) .addNode(new FilterNode("filter", "tick", 3)) .addNode(new
     * PrintNode("printer"));
     */
    public Flow addNode(AbstractNode node) {
        nodes.put(node.getId(), node);
        return this;
    }


    /**
     * connection 생성
     *
     * @param sourceNodeId 출발지 노드 아이디
     * @param sourcePort   출발지 포트 (out)
     * @param targetNodeId 도착지 노드 아이디
     * @param targetPort   도착지 포트 (in)
     * @return 메서드 체이닝 지원으로 this 호출 flow.connect("timer", "out", "filter", "in") .connect("filter", "out", "printer",
     * "in");
     */
    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort) {
        //node
        AbstractNode sourceNode = nodes.get(sourceNodeId);
        AbstractNode targetNode = nodes.get(targetNodeId);

        //node validation
        if (Objects.isNull(sourceNode)) {
            throw new IllegalArgumentException("sourceNode가 null 입니다.(ID:" + sourceNodeId + ")");
        }
        if (Objects.isNull(targetNode)) {
            throw new IllegalArgumentException("targetNode가 null 입니다.(ID:" + targetNodeId + ")");
        }

        //port
        OutputPort outPort = sourceNode.getOutputPort(sourcePort);
        InputPort inPort = targetNode.getInputPort(targetPort);

        //Port validation
        if (Objects.isNull(outPort)) {
            throw new IllegalArgumentException("sourcePort가 null 입니다.(ID:" + sourcePort + ")");
        }
        if (Objects.isNull(inPort)) {
            throw new IllegalArgumentException("targetPort가 null 입니다.(ID:" + targetPort + ")");
        }

        //1. Connection Id: "소스ID:포트->대상ID:포트" 형식
        String connId = String.format("%s:%s->%s:%s", sourceNodeId, sourcePort, targetNodeId, targetPort);

        //2. Connection 생성
        Connection conn = new Connection(connId);

        //3. outputPort -> Connection
        outPort.connect(conn);

        //4. Connection -> InputPort
        conn.setTarget(inPort);

        //5. 연결이 설정된 Connection을 connection List에 추가해둠!
        connections.add(conn);

        return this;
    }

    /**
     * 모든 노드의 initialize() 호출
     */
    public void initialize() {
        for (AbstractNode an : nodes.values()) {
            an.initialize();
        }
        this.state=FlowState.RUNNING;
    }

    /**
     * 모든 노드의 shutdown() 호출
     */
    public void shutdown() {
        for (AbstractNode an : nodes.values()) {
            an.shutdown();
        }
        this.state=FlowState.STOPPED;
    }

    /**
     * @return nodes values list
     */
    public List<AbstractNode> getNodes() {
        return nodes.values().stream().toList();
    }

    // getConnections()는 롬복으로 해결

    /**
     * Flow에 대한 validate
     * Flow에 등록된 노드가 있는 지
     * target이 설정되지않은 connection이 있는 지
     * Flow가 순환 참조를 하는 지
     * @return
     */

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (nodes.isEmpty()) {
            errors.add("플로우에 등록된 노드가 하나도 없습니다.");
        }

        for (Connection connection : connections) {
            if (connection.getTarget() == null) {
                errors.add("target이 설정되지 않은 파이프: " + connection.getId());
            }
        }

        if(hasCycle()){
            errors.add("플로우에 순환 참조가 존재");
        }
        return errors;
    }

    private boolean hasCycle() {
        // 그래프의 인접 리스트 (출발 노드 Id -> 도착 노드 Id 목록)
        Map<String, List<String>> adjList = new HashMap<>();

        for (String nodeId : nodes.keySet()) {
            adjList.put(nodeId, new ArrayList<>());
        }

        for (Connection conn : connections) {
            String[] parts = conn.getId().split("->");
            if (parts.length == 2) {
                String sourceId = parts[0].split(":")[0];
                String targetId = parts[1].split(":")[0];
                if (adjList.containsKey(sourceId)) {
                    adjList.get(sourceId).add(targetId);
                }
            }
        }

        //노드들을 UNVISITED 상태로 초기화
        Map<String, VisitStat> states = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            states.put(nodeId, VisitStat.UNVISITED);
        }

        for (String nodeId : nodes.keySet()) {
            if (states.get(nodeId) == VisitStat.UNVISITED) { //각 노드를 돌면서 DFS 시작
                if (dfs(nodeId, adjList, states)) {
                    return true; //순환참조 발견 시
                }
            }
        }
        return false;
    }

    /**
     * 깊이 우선 탐색 로직
     * @param nodeId 시작 노드
     * @param adjList 그래프 인접 리스트
     * @param states 노드별 상태
     * @return 순환참조 시 true 아니면 false
     */
    private boolean dfs(String nodeId, Map<String, List<String>> adjList, Map<String, VisitStat> states) {
        //탐색 중이라고 표시
        states.put(nodeId, VisitStat.VISITING);

        // 해당 노드의 인접 노드 탐색
        for (String neighbor : adjList.get(nodeId)) {
            VisitStat neighborState = states.get(neighbor);
            //만약 노드의 인접한 이웃의 상태가 VISITING이라면 순환참조라는 뜻이므로 true 리턴
            if (neighborState == VisitStat.VISITING) {
                return true;
            } else if (neighborState == VisitStat.UNVISITED) { // UNVISITED 상태면 다음 이웃 호출
                if (dfs(neighbor, adjList, states)) {
                    return true;
                }
            }
        }
        states.put(nodeId, VisitStat.VISITED); //true가 리턴 안된 상태이므로 해당 노드는 순환참조 안함 따라서 VISITED 하고 false 리턴
        return false;

    }
}
