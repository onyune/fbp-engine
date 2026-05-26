package com.fbp.engine.engine.cli.command;

import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "monitor", description = "Monitoring commands", subcommands = {
        MonitorCommand.FlowMonitorCommand.class,
        MonitorCommand.NodeMonitorCommand.class,
        MonitorCommand.DataMonitorCommand.class,
        MonitorCommand.StopMonitorCommand.class
})
public class MonitorCommand {
    private final FlowManager flowManager;
    // 활성 모니터링 세션 저장 (ID -> 리스너)
    private static final Map<String, Consumer<String>> activeSessions = new ConcurrentHashMap<>();

    public MonitorCommand(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @Command(name = "flow", description = "Monitor flow execution in background")
    public static class FlowMonitorCommand implements Runnable {
        @ParentCommand
        private MonitorCommand parent;
        @Parameters(index = "0") String id;
        @Override
        public void run() {
            if (parent.flowManager.getFlow(id) == null) {
                System.out.println("존재하지 않는 플로우 ID입니다: " + id);
                return;
            }

            if (activeSessions.containsKey(id)) {
                System.out.println("이미 모니터링 중인 ID입니다: " + id);
                return;
            }

            Consumer<String> listener = line -> {
                if (line.contains("flow_id=" + id)) {
                    System.out.println("\n[Monitor-" + id + "] " + line);
                    System.out.print("fbp> "); // 프롬프트 유지
                }
            };
            
            MetricsCollector.getInstance().addListener(listener);
            activeSessions.put(id, listener);
            System.out.println("플로우 [" + id + "] 백그라운드 모니터링 시작. (중단: monitor stop " + id + ")");
        }
    }

    @Command(name = "node", description = "Monitor node execution in background")
    public static class NodeMonitorCommand implements Runnable {
        @ParentCommand
        private MonitorCommand parent;
        @Parameters(index = "0") String id;
        @Override
        public void run() {
            boolean nodeExists = parent.flowManager.list().stream()
                    .flatMap(f -> f.getNodes().stream())
                    .anyMatch(n -> n.getId().equals(id));

            if (!nodeExists) {
                System.out.println("존재하지 않는 노드 ID입니다: " + id);
                return;
            }

            if (activeSessions.containsKey(id)) {
                System.out.println("이미 모니터링 중인 ID입니다: " + id);
                return;
            }

            Consumer<String> listener = line -> {
                if (line.contains("node_id=" + id)) {
                    System.out.println("\n[Monitor-" + id + "] " + line);
                    System.out.print("fbp> ");
                }
            };
            
            MetricsCollector.getInstance().addListener(listener);
            activeSessions.put(id, listener);
            System.out.println("노드 [" + id + "] 백그라운드 모니터링 시작. (중단: monitor stop " + id + ")");
        }
    }

    @Command(name = "data", description = "Monitor data with filter in background")
    public static class DataMonitorCommand implements Runnable {
        @ParentCommand
        private MonitorCommand parent;
        @Parameters(index = "0") String filter;
        @Override
        public void run() {
            if (activeSessions.containsKey(filter)) {
                System.out.println("이미 모니터링 중인 필터입니다: " + filter);
                return;
            }

            Consumer<String> listener = line -> {
                if (line.contains(filter)) {
                    System.out.println("\n[Monitor-" + filter + "] " + line);
                    System.out.print("fbp> ");
                }
            };
            
            MetricsCollector.getInstance().addListener(listener);
            activeSessions.put(filter, listener);
            System.out.println("필터 [" + filter + "] 백그라운드 데이터 모니터링 시작. (중단: monitor stop " + filter + ")");
        }
    }

    @Command(name = "stop", description = "Stop a background monitoring session")
    public static class StopMonitorCommand implements Runnable {
        @Parameters(index = "0") String id;
        @Override
        public void run() {
            Consumer<String> listener = activeSessions.remove(id);
            if (listener != null) {
                MetricsCollector.getInstance().removeListener(listener);
                System.out.println("모니터링 중단 완료: " + id);
            } else {
                System.out.println("진행 중인 모니터링 세션을 찾을 수 없습니다: " + id);
            }
        }
    }
}
