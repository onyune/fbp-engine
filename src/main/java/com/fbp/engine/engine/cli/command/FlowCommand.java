package com.fbp.engine.engine.cli.command;

import com.fbp.engine.core.Flow;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.parser.NodeDefinition;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Command(name = "flow", description = "Flow management commands", subcommands = {
        FlowCommand.ListCommand.class,
        FlowCommand.DeployCommand.class,
        FlowCommand.StartCommand.class,
        FlowCommand.StopCommand.class,
        FlowCommand.RestartCommand.class,
        FlowCommand.RemoveCommand.class,
        FlowCommand.StatusCommand.class,
        FlowCommand.PatchCommand.class,
        FlowCommand.AddNodeCommand.class,
        FlowCommand.RemoveNodeCommand.class,
        FlowCommand.AddWireCommand.class,
        FlowCommand.RemoveWireCommand.class,
        FlowCommand.UpdateConfigCommand.class,
        FlowCommand.HistoryCommand.class,
        FlowCommand.RollbackCommand.class
})
public class FlowCommand {
    private final FlowManager flowManager;

    public FlowCommand(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public FlowManager getFlowManager() {
        return flowManager;
    }

    @Command(name = "list", description = "List all flows")
    public static class ListCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;

        @Override
        public void run() {
            System.out.println(String.format("%-25s %-10s %-10s %-6s %-6s", "ID", "STATUS", "TRANSPORT", "NODES", "WIRES"));
            System.out.println("-----------------------------------------------------------------------");
            for (Flow f : parent.getFlowManager().list()) {
                String transport = "local";
                List<FlowDefinition> history = parent.getFlowManager().getRawHistory(f.getId());
                if (history != null && !history.isEmpty()) {
                    FlowDefinition latest = history.get(history.size() - 1);
                    if (latest.transport() != null) transport = latest.transport().type();
                }
                System.out.println(String.format("%-25s %-10s %-10s %-6d %-6d",
                        f.getId(), f.getState(), transport, f.getNodes().size(), f.getConnections().size()));
            }
        }
    }

    @Command(name = "status", description = "Show flow status")
    public static class StatusCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;

        @Override
        public void run() {
            Flow f = parent.getFlowManager().getFlow(id);
            if (f == null) { System.out.println("플로우를 찾을 수 없습니다: " + id); return; }

            String transportStr = "local";
            List<FlowDefinition> history = parent.getFlowManager().getRawHistory(f.getId());
            if (history != null && !history.isEmpty()) {
                FlowDefinition latest = history.get(history.size() - 1);
                if (latest.transport() != null) {
                    transportStr = String.format("%s (%s, QoS=%d)", latest.transport().type(), latest.transport().broker(), latest.transport().qos());
                }
            }

            long processed = 0;
            long errors = 0;
            for (com.fbp.engine.node.Node n : f.getNodes()) {
                com.fbp.engine.metrics.NodeMetrics nm = com.fbp.engine.metrics.MetricsCollector.getInstance().getMetrics(id, n.getId());
                processed += nm.getOutCount();
                errors += nm.getErrorCount();
            }
            double errorPct = (processed + errors) == 0 ? 0 : (double) errors / (processed + errors) * 100;

            System.out.println("Flow: " + f.getId());
            System.out.println("  Status:    " + f.getState());
            System.out.println("  Transport: " + transportStr);
            System.out.println("  Nodes:     " + f.getNodes().size());
            System.out.println("  Wires:     " + f.getConnections().size());
            System.out.println("  Uptime:    -");
            System.out.printf("  Processed: %,d messages\n", processed);
            System.out.printf("  Errors:    %,d (%.3f%%)\n", errors, errorPct);
            System.out.println("  Revision:  " + (history != null ? history.size() : 0));
        }
    }

    @Command(name = "deploy", description = "Deploy flow from file")
    public static class DeployCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String file;

        @Override
        public void run() {
            try (InputStream fis = Files.newInputStream(Paths.get(file))) {
                FlowDefinition def = new JsonFlowParser().parse(fis);
                parent.getFlowManager().deploy(def);
                System.out.println("플로우 배포 성공: " + def.id());
            } catch (java.nio.file.NoSuchFileException e) {
                System.out.println("파일을 찾을 수 없습니다: " + file);
            } catch (Exception e) {
                System.out.println("배포 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "start", description = "Start flow")
    public static class StartCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;

        @Override
        public void run() {
            try {
                parent.getFlowManager().restart(id);
                System.out.println("플로우 시작 완료: " + id);
            } catch (Exception e) {
                System.out.println("플로우 시작 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "stop", description = "Stop flow")
    public static class StopCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;

        @Override
        public void run() {
            try {
                parent.getFlowManager().stop(id);
                System.out.println("플로우 정지 완료: " + id);
            } catch (Exception e) {
                System.out.println("플로우 정지 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "restart", description = "Restart flow")
    public static class RestartCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;

        @Override
        public void run() {
            try {
                parent.getFlowManager().stop(id);
                parent.getFlowManager().restart(id);
                System.out.println("플로우 재시작 완료: " + id);
            } catch (Exception e) {
                System.out.println("플로우 재시작 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "remove", description = "Remove flow")
    public static class RemoveCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;

        @Override
        public void run() {
            try {
                parent.getFlowManager().remove(id);
                System.out.println("플로우 삭제 완료: " + id);
            } catch (Exception e) {
                System.out.println("플로우 삭제 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "patch", description = "Dynamic patch flow")
    public static class PatchCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;
        @Parameters(index = "1") String file;

        @Override
        public void run() {
            try (InputStream fis = Files.newInputStream(Paths.get(file))) {
                FlowDefinition newDef = new JsonFlowParser().parse(fis);
                
                System.out.println("Calculating diff...");
                // Note: Full diff calculation is done inside flowManager.patch
                parent.getFlowManager().patch(id, newDef);
                
                List<FlowDefinition> history = parent.getFlowManager().getRawHistory(id);
                System.out.println("Patch applied. Revision: " + (history != null ? history.size() : "?"));
            } catch (java.nio.file.NoSuchFileException e) {
                System.out.println("파일을 찾을 수 없습니다: " + file);
            } catch (Exception e) {
                System.out.println("패치 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "add-node", description = "Add node to flow")
    public static class AddNodeCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String flowId;
        @Parameters(index = "1") String nodeSpec;

        @Override
        public void run() {
            try {
                String[] nodeInfo = nodeSpec.split(":");
                if(nodeInfo.length != 2) throw new IllegalArgumentException("형식 오류. 예: myNode:GeneratorNode");

                NodeDefinition nDef = new NodeDefinition(nodeInfo[0], nodeInfo[1], Map.of());
                parent.getFlowManager().addNode(flowId, nDef);
                System.out.println("노드 동적 추가 성공: " + nodeInfo[0]);
            } catch (Exception e) {
                System.out.println("노드 추가 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "remove-node", description = "Remove node from flow")
    public static class RemoveNodeCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String flowId;
        @Parameters(index = "1") String nodeId;

        @Override
        public void run() {
            try {
                parent.getFlowManager().removeNode(flowId, nodeId);
                System.out.println("노드 동적 제거 성공: " + nodeId);
            } catch (Exception e) {
                System.out.println("노드 제거 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "add-wire", description = "Add wire to flow")
    public static class AddWireCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String flowId;
        @Parameters(index = "1") String from;
        @Parameters(index = "2") String to;

        @Override
        public void run() {
            try {
                String[] fromParts = from.split(":");
                String[] toParts = to.split(":");
                if(fromParts.length != 2 || toParts.length != 2) throw new IllegalArgumentException("형식 오류. 예: gen1:out log1:in");

                ConnectionDefinition cDef = new ConnectionDefinition(fromParts[0], fromParts[1], toParts[0], toParts[1]);
                parent.getFlowManager().addConnection(flowId, cDef, null);
                System.out.println("와이어 동적 연결 성공");
            } catch (Exception e) {
                System.out.println("와이어 연결 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "remove-wire", description = "Remove wire from flow")
    public static class RemoveWireCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String flowId;
        @Parameters(index = "1") String wireId;

        @Override
        public void run() {
            try {
                parent.getFlowManager().removeConnection(flowId, wireId);
                System.out.println("와이어 동적 제거 성공: " + wireId);
            } catch (Exception e) {
                System.out.println("와이어 제거 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "update-config", description = "Update node config")
    public static class UpdateConfigCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String flowId;
        @Parameters(index = "1") String nodeId;
        @Parameters(index = "2..*") String[] configArgs;

        @Override
        public void run() {
            try {
                String configStr = String.join(" ", configArgs);
                Map<String, Object> newConfig = new com.fasterxml.jackson.databind.ObjectMapper().readValue(configStr, Map.class);
                parent.getFlowManager().updateNodeConfig(flowId, nodeId, newConfig);
                System.out.println("노드 설정 변경 성공: " + nodeId);
            } catch (Exception e) {
                System.out.println("노드 설정 변경 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "history", description = "Show flow history")
    public static class HistoryCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;

        @Override
        public void run() {
            try {
                List<String> logs = parent.getFlowManager().getHistory(id);
                System.out.println("=== [" + id + "] 변경 이력 ===");
                for (String log : logs) {
                    System.out.println(log);
                }
            } catch (Exception e) {
                System.out.println("이력 조회 실패: " + e.getMessage());
            }
        }
    }

    @Command(name = "rollback", description = "Rollback flow to revision")
    public static class RollbackCommand implements Runnable {
        @ParentCommand
        private FlowCommand parent;
        @Parameters(index = "0") String id;
        @Parameters(index = "1") int rev;

        @Override
        public void run() {
            try {
                parent.getFlowManager().rollback(id, rev);
                System.out.println("롤백 성공: Revision " + rev + " 버전으로 복구되었습니다.");
            } catch (Exception e) {
                System.out.println("롤백 실패: " + e.getMessage());
            }
        }
    }
}
