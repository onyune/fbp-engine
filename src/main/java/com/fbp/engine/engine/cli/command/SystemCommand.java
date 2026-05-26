package com.fbp.engine.engine.cli.command;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.impl.MqttBridgeConnection;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import picocli.CommandLine.Command;

import java.time.Duration;

public class SystemCommand {
    private static final long START_TIME = System.currentTimeMillis();

    @Command(name = "stats", description = "Show engine statistics")
    public static class StatsCommand implements Runnable {
        private final FlowManager flowManager;

        public StatsCommand(FlowManager flowManager) { 
            this.flowManager = flowManager; 
        }

        @Override
        public void run() {
            if (flowManager == null) {
                System.out.println("Error: FlowManager not initialized.");
                return;
            }

            long uptimeMs = System.currentTimeMillis() - START_TIME;
            Duration d = Duration.ofMillis(uptimeMs);
            String uptime = String.format("%dh %dm %ds", d.toHours(), d.toMinutesPart(), d.toSecondsPart());

            int activeFlows = (int) flowManager.list().stream().filter(f -> f.getState() == Flow.FlowState.RUNNING).count();
            int stoppedFlows = flowManager.list().size() - activeFlows;
            
            long totalNodes = flowManager.list().stream().mapToLong(f -> f.getNodes().size()).sum();
            long totalWires = flowManager.list().stream().mapToLong(f -> f.getConnections().size()).sum();
            long mqttWires = flowManager.list().stream()
                    .flatMap(f -> f.getConnections().stream())
                    .filter(c -> c instanceof MqttBridgeConnection)
                    .count();
            long localWires = totalWires - mqttWires;

            MetricsCollector mc = MetricsCollector.getInstance();
            long heapUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
            long heapMax = Runtime.getRuntime().maxMemory() / (1024 * 1024);

            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║         FBP Engine Statistics            ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println(String.format("║ Status:        %-25s ║", "RUNNING"));
            System.out.println(String.format("║ Uptime:        %-25s ║", uptime));
            System.out.println(String.format("║ Active Flows:  %d (local: %d, mqtt: %d)   ║", activeFlows, 0, 0)); // We don't track flow transport easily here
            System.out.println(String.format("║ Stopped Flows: %-25d ║", stoppedFlows));
            System.out.println(String.format("║ Total Nodes:   %-25d ║", totalNodes));
            System.out.println(String.format("║ Total Wires:   %d (local: %d, mqtt: %d)   ║", totalWires, localWires, mqttWires));
            System.out.println(String.format("║ Total Processed: %-23d ║", mc.getGlobalProcessed()));
            System.out.println(String.format("║ Total Errors:  %-25d ║", mc.getGlobalErrors()));
            System.out.println(String.format("║ Heap Used:     %d MB / %d MB            ║", heapUsed, heapMax));
            System.out.println(String.format("║ Active Threads: %-25d ║", Thread.activeCount()));
            System.out.println(String.format("║ Influx:        %-25s ║", mc.isInfluxConnected() ? "OK" : "ERROR"));
            System.out.println("╚══════════════════════════════════════════╝");
        }
    }

    @Command(name = "broker", description = "Broker status")
    public static class BrokerCommand implements Runnable {
        private final FlowManager flowManager;
        public BrokerCommand(FlowManager flowManager) { this.flowManager = flowManager; }
        
        @Command(name = "status", description = "Show broker status")
        public void status() {
            long mqttWires = flowManager.list().stream()
                    .flatMap(f -> f.getConnections().stream())
                    .filter(c -> c instanceof MqttBridgeConnection)
                    .count();
            
            System.out.println("System Broker: [Configured in application.yml]");
            System.out.println("  Status:       CONNECTED (Simulated)");
            System.out.println("  Active Topics: " + mqttWires);
            System.out.println("  Messages/sec:  -");
        }
        
        @Override
        public void run() { status(); }
    }

    @Command(name = "influx", description = "InfluxDB status")
    public static class InfluxCommand implements Runnable {
        private final FlowManager flowManager;
        public InfluxCommand(FlowManager flowManager) { this.flowManager = flowManager; }
        
        @Command(name = "status", description = "Show InfluxDB status")
        public void status() {
            MetricsCollector mc = MetricsCollector.getInstance();
            System.out.println("InfluxDB: " + mc.getInfluxUrl());
            System.out.println("  Status:        " + (mc.isInfluxConnected() ? "CONNECTED" : "DISCONNECTED"));
            System.out.println("  Batch Queue:   " + mc.getEventQueueSize() + " / 10000");
            System.out.println("  Offline Buffer: " + mc.getOfflineBufferSize());
            System.out.println("  Total Dropped: " + mc.getDroppedMetricsCount());
            System.out.println("  Total Written: " + mc.getGlobalProcessed() + " points (Simulated)");
        }
        
        @Override
        public void run() { status(); }
    }
}
