package com.fbp.engine.cli;

import com.fbp.engine.core.Flow;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetrics;
import com.fbp.engine.node.Node;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import java.io.InputStream;
import java.util.Scanner;

public class FbpCli {
    private final FlowManager flowManager;

    public FbpCli(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=========================================");
        System.out.println(" FBP IoT Rule Engine CLI Started!        ");
        System.out.println(" Type 'help' to see available commands   ");
        System.out.println("=========================================\n");

        while (true) {
            System.out.print("fbp> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] args = line.split("\\s+");
            String cmd = args[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help": printHelp(); break;
                    case "exit":
                    case "quit":
                        System.out.println("엔진 CLI를 종료합니다.");
                        System.exit(0);
                        break;
                    case "flow": handleFlowCommand(args); break;
                    case "node": handleNodeCommand(args); break;
                    case "wire": handleWireCommand(args); break;
                    case "sensor": handleSensorCommand(args); break;
                    case "monitor": handleMonitorCommand(args); break;
                    case "stats": handleStatsCommand(); break;
                    case "broker": handleBrokerCommand(args); break;
                    case "influx": handleInfluxCommand(args); break;
                    default: System.out.println("알 수 없는 명령어입니다: " + cmd);
                }
            } catch (Exception e) {
                System.out.println("실행 중 에러 발생: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("================== [ 명령어 목록 ] ==================");
        System.out.println("[플로우]");
        System.out.println("  flow list");
        System.out.println("  flow deploy <file>");
        System.out.println("  flow start <id>");
        System.out.println("  flow stop <id>");
        System.out.println("  flow restart <id>");
        System.out.println("  flow remove <id>");
        System.out.println("  flow status <id>");
        System.out.println("  flow patch <id> <file>");
        System.out.println("  flow add-node <id> <spec>");
        System.out.println("  flow remove-node <id> <node-id>");
        System.out.println("  flow add-wire <id> <from> <to>");
        System.out.println("  flow remove-wire <id> <wire-id>");
        System.out.println("  flow update-config <id> <node-id> <config>");
        System.out.println("  flow history <id>");
        System.out.println("  flow rollback <id> <rev>");
        System.out.println("[노드]");
        System.out.println("  node list <flow-id>");
        System.out.println("  node info <node-id>");
        System.out.println("  node stats <node-id>");
        System.out.println("[와이어]");
        System.out.println("  wire list <flow-id>");
        System.out.println("  wire info <wire-id>");
        System.out.println("  wire stats <wire-id>");
        System.out.println("[모니터링]");
        System.out.println("  monitor flow <id>");
        System.out.println("  monitor node <id>");
        System.out.println("  monitor data <id> --filter <expr>");
        System.out.println("[도메인 통계]");
        System.out.println("  sensor list");
        System.out.println("  sensor stats <name> [--window 1h] [--range 7d]");
        System.out.println("[시스템]");
        System.out.println("  stats");
        System.out.println("  broker status");
        System.out.println("  influx status");
        System.out.println("  exit, quit");
        System.out.println("====================================================");
    }

    // ==========================================
    // FLOW 명령어 그룹
    // ==========================================
    private void handleFlowCommand(String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: flow <list|deploy|start|stop|restart|remove|status|patch...>");
            return;
        }
        String subCmd = args[1].toLowerCase();

        switch (subCmd) {
            case "list":
                System.out.println(String.format("%-25s %-10s %-10s %-10s", "ID", "STATUS", "NODES", "WIRES"));
                System.out.println("---------------------------------------------------------------");
                for (Flow f : flowManager.list()) {
                    System.out.println(String.format("%-25s %-10s %-10d %-10d",
                            f.getId(), f.getState(), f.getNodes().size(), f.getConnections().size()));
                }
                break;
            case "status":
                if (args.length < 3) { System.out.println("사용법: flow status <id>"); return; }
                Flow f = flowManager.getFlow(args[2]);
                if (f == null) { System.out.println("플로우를 찾을 수 없습니다: " + args[2]); return; }
                System.out.println("Flow: " + f.getId());
                System.out.println("  Status:    " + f.getState());
                System.out.println("  Nodes:     " + f.getNodes().size());
                System.out.println("  Wires:     " + f.getConnections().size());
                break;
            case "deploy":
                if (args.length < 3) { System.out.println("사용법: flow deploy <file>"); return; }
                deployFlowFromFile(args[2]);
                break;
            case "start":
                if (args.length < 3) { System.out.println("사용법: flow start <id>"); return; }
                System.out.println("[미구현] 플로우 시작: " + args[2]);
                break;
            case "stop":
                if (args.length < 3) { System.out.println("사용법: flow stop <id>"); return; }
                System.out.println("[미구현] 플로우 정지: " + args[2]);
                break;
            case "restart":
                if (args.length < 3) { System.out.println("사용법: flow restart <id>"); return; }
                System.out.println("[미구현] 플로우 재시작: " + args[2]);
                break;
            case "remove":
                if (args.length < 3) { System.out.println("사용법: flow remove <id>"); return; }
                System.out.println("[미구현] 플로우 삭제: " + args[2]);
                break;
            case "patch":
                if (args.length < 4) { System.out.println("사용법: flow patch <id> <file>"); return; }
                System.out.println("[미구현] 플로우 동적 패치 적용 - ID: " + args[2] + ", File: " + args[3]);
                break;
            case "add-node":
                if (args.length < 4) { System.out.println("사용법: flow add-node <id> <spec>"); return; }
                System.out.println("[미구현] 노드 추가 - Flow: " + args[2] + ", Spec: " + args[3]);
                break;
            case "remove-node":
                if (args.length < 4) { System.out.println("사용법: flow remove-node <id> <node-id>"); return; }
                System.out.println("[미구현] 노드 제거 - Flow: " + args[2] + ", Node: " + args[3]);
                break;
            case "add-wire":
                if (args.length < 5) { System.out.println("사용법: flow add-wire <id> <from> <to>"); return; }
                System.out.println("[미구현] 연결 추가 - Flow: " + args[2] + ", From: " + args[3] + " To: " + args[4]);
                break;
            case "remove-wire":
                if (args.length < 4) { System.out.println("사용법: flow remove-wire <id> <wire-id>"); return; }
                System.out.println("[미구현] 연결 제거 - Flow: " + args[2] + ", Wire: " + args[3]);
                break;
            case "update-config":
                if (args.length < 5) { System.out.println("사용법: flow update-config <id> <node-id> <config>"); return; }
                System.out.println("[미구현] 노드 설정 변경 - Flow: " + args[2] + ", Node: " + args[3] + ", Config: " + args[4]);
                break;
            case "history":
                if (args.length < 3) { System.out.println("사용법: flow history <id>"); return; }
                System.out.println("[미구현] 변경 이력 조회 - Flow: " + args[2]);
                break;
            case "rollback":
                if (args.length < 4) { System.out.println("사용법: flow rollback <id> <rev>"); return; }
                System.out.println("[미구현] 롤백 - Flow: " + args[2] + ", Revision: " + args[3]);
                break;
            default:
                System.out.println("알 수 없는 flow 명령어입니다: " + subCmd);
        }
    }

    // ==========================================
    // NODE 명령어 그룹
    // ==========================================
    private void handleNodeCommand(String[] args) {
        if (args.length < 3) { System.out.println("사용법: node <list|info|stats> <id>"); return; }
        String subCmd = args[1].toLowerCase();
        String targetId = args[2];

        switch (subCmd) {
            case "list":
                Flow f = flowManager.getFlow(targetId);
                if (f == null) { System.out.println("플로우를 찾을 수 없습니다: " + targetId); return; }
                System.out.println("Nodes in [" + targetId + "]:");
                for (Node n : f.getNodes()) {
                    System.out.println(" - " + n.getId() + " (" + n.getClass().getSimpleName() + ")");
                }
                break;
            case "info":
                System.out.println("[미구현] 노드 상세(Config, 포트 목록) 조회 - Node: " + targetId);
                break;
            case "stats":
                String flowId = null;
                for (Flow flow : flowManager.list()) {
                    if (flow.getNodes().stream().anyMatch(n -> n.getId().equals(targetId))) {
                        flowId = flow.getId(); break;
                    }
                }
                if (flowId == null) { System.out.println("활성화된 플로우에서 노드를 찾을 수 없습니다."); return; }

                NodeMetrics nm = MetricsCollector.getInstance().getMetrics(flowId, targetId);
                System.out.println("Node: " + targetId);
                System.out.println("  Processed:  " + nm.getProcessedCount());
                System.out.println("  Errors:     " + nm.getErrorCount());
                System.out.printf("  Avg Time:   %.2f ms\n", nm.getAverageTime());
                System.out.println("  P99 Time:   " + nm.getP99Time() + " ms");
                break;
            default: System.out.println("알 수 없는 node 명령어입니다.");
        }
    }

    // ==========================================
    //  WIRE 명령어 그룹
    // ==========================================
    private void handleWireCommand(String[] args) {
        if (args.length < 3) { System.out.println("사용법: wire <list|info|stats> <id>"); return; }
        String subCmd = args[1].toLowerCase();
        String targetId = args[2];

        switch (subCmd) {
            case "list":
                System.out.println("[미구현] 플로우 내 연결 목록 조회 - Flow: " + targetId);
                break;
            case "info":
                System.out.println("[미구현] 연결 상세 정보 조회 - Wire: " + targetId);
                break;
            case "stats":
                System.out.println("[미구현] 연결 통계 조회 - Wire: " + targetId);
                break;
            default: System.out.println("알 수 없는 wire 명령어입니다.");
        }
    }

    // ==========================================
    //  SENSOR 명령어 그룹
    // ==========================================
    private void handleSensorCommand(String[] args) {
        if (args.length < 2) { System.out.println("사용법: sensor <list|stats> ..."); return; }
        String subCmd = args[1].toLowerCase();

        if ("list".equals(subCmd)) {
            System.out.println("[미구현] 등록된 도메인 메트릭(센서) 목록 조회");
        } else if ("stats".equals(subCmd)) {
            if (args.length < 3) { System.out.println("사용법: sensor stats <name> [--window 1h] [--range 7d]"); return; }

            // 파라미터 파싱
            String sensorName = args[2];
            String window = "1h"; // 기본값
            String range = "24h"; // 기본값

            for (int i = 3; i < args.length; i++) {
                if ("--window".equals(args[i]) && i + 1 < args.length) {
                    window = args[i + 1];
                    i++;
                } else if ("--range".equals(args[i]) && i + 1 < args.length) {
                    range = args[i + 1];
                    i++;
                }
            }
            System.out.println("[미구현] InfluxDB 도메인 통계 쿼리 - Sensor: " + sensorName + ", Window: " + window + ", Range: " + range);
        } else {
            System.out.println("알 수 없는 sensor 명령어입니다.");
        }
    }

    // ==========================================
    // MONITOR 명령어 그룹
    // ==========================================
    private void handleMonitorCommand(String[] args) {
        if (args.length < 3) { System.out.println("사용법: monitor <flow|node|data> <id> [--filter <expr>]"); return; }
        String subCmd = args[1].toLowerCase();
        String targetId = args[2];

        switch (subCmd) {
            case "flow":
                System.out.println("[미구현] 플로우 실시간 메시지 흐름 추적 (tail -f) - Flow: " + targetId);
                break;
            case "node":
                System.out.println("[미구현] 노드 입출력 메시지 실시간 추적 - Node: " + targetId);
                break;
            case "data":
                String filter = "none";
                if (args.length >= 5 && "--filter".equals(args[3])) {
                    filter = args[4];
                }
                System.out.println("[미구현] 조건 필터링 실시간 모니터링 - ID: " + targetId + ", Filter: " + filter);
                break;
            default: System.out.println("알 수 없는 monitor 명령어입니다.");
        }
    }

    // ==========================================
    //  STATS (엔진 전체 통계)
    // ==========================================
    private void handleStatsCommand() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         FBP Engine Statistics            ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║ Status:        RUNNING                   ║");
        System.out.println("║ Active Flows:  " + flowManager.list().size() + "                         ║");

        long heapUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long heapMax = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        System.out.println("║ Heap Used:     " + heapUsed + " MB / " + heapMax + " MB          ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ==========================================
    // BROKER / INFLUX STATUS
    // ==========================================
    private void handleBrokerCommand(String[] args) {
        if (args.length > 1 && "status".equals(args[1].toLowerCase())) {
            System.out.println("[미구현] 시스템 브로커 연결 상태 및 Active Topic 수 조회 로직");
        } else {
            System.out.println("사용법: broker status");
        }
    }

    private void handleInfluxCommand(String[] args) {
        if (args.length > 1 && "status".equals(args[1].toLowerCase())) {
            System.out.println("[미구현] InfluxDB 연결 상태, 배치 큐 적체량, 누적 적재량 조회 로직");
        } else {
            System.out.println("사용법: influx status");
        }
    }

    private void deployFlowFromFile(String filePath) {
        try (InputStream fis = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(filePath))) {

            JsonFlowParser parser = new com.fbp.engine.parser.JsonFlowParser();

            FlowDefinition def = parser.parse(fis);

            // 엔진에 배포
            flowManager.deploy(def);
            System.out.println("플로우 배포 성공: " + def.id());

        } catch (java.nio.file.NoSuchFileException e) {
            System.out.println("파일을 찾을 수 없습니다: " + filePath);
        } catch (Exception e) {
            System.out.println("배포 실패: " + e.getMessage());
        }
    }
}