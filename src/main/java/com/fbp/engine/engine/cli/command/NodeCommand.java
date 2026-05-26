package com.fbp.engine.engine.cli.command;

import com.fbp.engine.core.Flow;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetrics;
import com.fbp.engine.node.Node;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "node", description = "Node management commands", subcommands = {
        NodeCommand.ListCommand.class,
        NodeCommand.InfoCommand.class,
        NodeCommand.StatsCommand.class
})
public class NodeCommand {
    private final FlowManager flowManager;

    public NodeCommand(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public FlowManager getFlowManager() {
        return flowManager;
    }

    @Command(name = "list", description = "List nodes in a flow")
    public static class ListCommand implements Runnable {
        @ParentCommand
        private NodeCommand parent;
        @Parameters(index = "0") String flowId;

        @Override
        public void run() {
            Flow f = parent.getFlowManager().getFlow(flowId);
            if (f == null) { System.out.println("플로우를 찾을 수 없습니다: " + flowId); return; }
            System.out.println("Nodes in [" + flowId + "]:");
            for (Node n : f.getNodes()) {
                System.out.println(" - " + n.getId() + " (" + n.getClass().getSimpleName() + ")");
            }
        }
    }

    @Command(name = "info", description = "Show node info")
    public static class InfoCommand implements Runnable {
        @ParentCommand
        private NodeCommand parent;
        @Parameters(index = "0") String nodeId;

        @Override
        public void run() {
            Node target = null;
            Flow targetFlow = null;
            for (Flow flow : parent.getFlowManager().list()) {
                target = flow.getNodes().stream().filter(n -> n.getId().equals(nodeId)).findFirst().orElse(null);
                if (target != null) {
                    targetFlow = flow;
                    break;
                }
            }

            if (target == null) { System.out.println("노드를 찾을 수 없습니다: " + nodeId); return; }

            System.out.println("Node: " + nodeId + " (" + target.getClass().getSimpleName() + ")");
            System.out.println("  Flow:       " + targetFlow.getId());
            if (target instanceof com.fbp.engine.node.AbstractNode) {
                com.fbp.engine.node.AbstractNode abs = (com.fbp.engine.node.AbstractNode) target;
                System.out.println("  Config:     " + abs.getConfig());
                System.out.println("  Input Ports:  " + abs.getInputPorts().keySet());
                System.out.println("  Output Ports: " + abs.getOutputPorts().keySet());
            }

            System.out.println("  Wires:");
            for (com.fbp.engine.core.Connection conn : targetFlow.getConnections()) {
                if (conn.getId().contains(nodeId + ":")) {
                    System.out.println("    - " + conn.getId());
                }
            }
        }
    }

    @Command(name = "stats", description = "Show node stats")
    public static class StatsCommand implements Runnable {
        @ParentCommand
        private NodeCommand parent;
        @Parameters(index = "0") String nodeId;

        @Override
        public void run() {
            Node target = null;
            String flowId = null;
            for (Flow flow : parent.getFlowManager().list()) {
                target = flow.getNodes().stream().filter(n -> n.getId().equals(nodeId)).findFirst().orElse(null);
                if (target != null) {
                    flowId = flow.getId(); break;
                }
            }
            if (flowId == null) { System.out.println("활성화된 플로우에서 노드를 찾을 수 없습니다."); return; }

            NodeMetrics nm = MetricsCollector.getInstance().getMetrics(flowId, nodeId);
            System.out.println("Node: " + nodeId + " (" + target.getClass().getSimpleName() + ")");
            System.out.println("  Status:     RUNNING"); // Assume running if found
            System.out.printf("  In:         %,d msg / %s\n", nm.getInCount(), formatBytes(nm.getInBytes()));
            System.out.printf("  Out:        %,d msg / %s\n", nm.getOutCount(), formatBytes(nm.getOutBytes()));
            
            long filtered = nm.getInCount() - nm.getOutCount() - nm.getErrorCount();
            double filteredPct = nm.getInCount() == 0 ? 0 : (double) filtered / nm.getInCount() * 100;
            System.out.printf("  Filtered:   %,d (%.1f%%)\n", filtered, filteredPct);
            System.out.println("  Errors:     " + nm.getErrorCount());
            System.out.printf("  Avg Time:   %.1f ms\n", nm.getAverageTime());
            System.out.println("  P99 Time:   " + nm.getP99Time() + " ms");
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            char pre = "KMGTPE".charAt(exp - 1);
            return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
        }
    }
}
