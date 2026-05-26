package com.fbp.engine.engine.cli.command;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.impl.LocalConnection;
import com.fbp.engine.core.impl.MqttBridgeConnection;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.WireMetrics;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "wire", description = "Wire management commands", subcommands = {
        WireCommand.ListCommand.class,
        WireCommand.InfoCommand.class,
        WireCommand.StatsCommand.class
})
public class WireCommand {
    private final FlowManager flowManager;

    public WireCommand(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public FlowManager getFlowManager() {
        return flowManager;
    }

    @Command(name = "list", description = "List wires in a flow")
    public static class ListCommand implements Runnable {
        @ParentCommand
        private WireCommand parent;
        @Parameters(index = "0") String flowId;

        @Override
        public void run() {
            Flow f = parent.getFlowManager().getFlow(flowId);
            if (f == null) { System.out.println("플로우를 찾을 수 없습니다: " + flowId); return; }

            System.out.println(String.format("%-5s %-15s %-15s %-10s %-45s %-5s", "ID", "FROM", "TO", "TRANSPORT", "TOPIC", "QUEUE"));
            int i = 1;
            for (Connection conn : f.getConnections()) {
                String id = "w-" + i++;
                String[] parts = conn.getId().split("->");
                String from = parts.length > 0 ? parts[0] : "???";
                String to = parts.length > 1 ? parts[1] : "???";
                String transport = (conn instanceof MqttBridgeConnection) ? "mqtt" : "local";
                String topic = (conn instanceof MqttBridgeConnection) ? ((MqttBridgeConnection)conn).getTopic() : "-";
                int queue = conn.getBufferSize();

                System.out.println(String.format("%-5s %-15s %-15s %-10s %-45s %-5d", id, from, to, transport, topic, queue));
            }
        }
    }

    @Command(name = "info", description = "Show wire info")
    public static class InfoCommand implements Runnable {
        @ParentCommand
        private WireCommand parent;
        @Parameters(index = "0") String wireAlias; // w-1 format or full ID

        @Override
        public void run() {
            Connection target = null;
            // Find connection by w-N alias or full ID
            for (Flow flow : parent.getFlowManager().list()) {
                int i = 1;
                for (Connection conn : flow.getConnections()) {
                    String alias = "w-" + i++;
                    if (alias.equals(wireAlias) || conn.getId().equals(wireAlias)) {
                        target = conn;
                        break;
                    }
                }
                if (target != null) break;
            }

            if (target == null) { System.out.println("연결을 찾을 수 없습니다: " + wireAlias); return; }

            String[] parts = target.getId().split("->");
            String from = parts.length > 0 ? parts[0] : "???";
            String to = parts.length > 1 ? parts[1] : "???";
            WireMetrics wm = MetricsCollector.getInstance().getWireMetrics(target.getId());

            System.out.println("Wire: " + wireAlias);
            System.out.println("  From:       " + from);
            System.out.println("  To:         " + to);
            if (target instanceof MqttBridgeConnection) {
                MqttBridgeConnection mbc = (MqttBridgeConnection) target;
                System.out.println("  Transport:  MqttBridge");
                System.out.println("  Broker:     " + mbc.getBrokerUrl());
                System.out.println("  Topic:      " + mbc.getTopic());
                System.out.println("  QoS:        " + mbc.getQos());
            } else {
                System.out.println("  Transport:  Local");
            }
            System.out.println("  Queue Size: " + target.getBufferSize() + " / 100");
            if (wm != null) {
                System.out.println("  Delivered:  " + wm.getDeliveredCount().sum());
                System.out.println("  Dropped:    " + wm.getDroppedCount().sum());
            } else {
                System.out.println("  Delivered:  0");
                System.out.println("  Dropped:    0");
            }
        }
    }

    @Command(name = "stats", description = "Show wire stats")
    public static class StatsCommand implements Runnable {
        @ParentCommand
        private WireCommand parent;
        @Parameters(index = "0") String wireAlias;

        @Override
        public void run() {
            InfoCommand info = new InfoCommand();
            info.parent = this.parent;
            info.wireAlias = this.wireAlias;
            info.run();
        }
    }
}
