package com.fbp.engine.engine.cli.command;

import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.MetricsCollector.SensorWindow;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Command(name = "sensor", description = "Sensor management commands", subcommands = {
        SensorCommand.ListCommand.class,
        SensorCommand.StatsCommand.class
})
public class SensorCommand {
    private final FlowManager flowManager;

    public SensorCommand(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @Command(name = "list", description = "List registered sensors")
    public static class ListCommand implements Runnable {
        @ParentCommand
        private SensorCommand parent;

        @Override
        public void run() {
            MetricsCollector mc = MetricsCollector.getInstance();
            System.out.println("Registered Sensors (Current active windows):");
            mc.getWindow1m().keySet().forEach(k -> System.out.println(" - " + k + " (1m window)"));
            mc.getWindow1h().keySet().forEach(k -> System.out.println(" - " + k + " (1h window)"));
        }
    }

    @Command(name = "stats", description = "Show sensor stats")
    public static class StatsCommand implements Runnable {
        @ParentCommand
        private SensorCommand parent;
        @Parameters(index = "0") String name;
        @Option(names = "--window", defaultValue = "1h") String window;
        @Option(names = "--range", defaultValue = "24h") String range;

        @Override
        public void run() {
            MetricsCollector mc = MetricsCollector.getInstance();
            Map<String, SensorWindow> targetMap;
            if ("1m".equals(window)) targetMap = mc.getWindow1m();
            else if ("1d".equals(window)) targetMap = mc.getWindow1d();
            else targetMap = mc.getWindow1h();

            System.out.println("Sensor: " + name);
            System.out.println("  Window: " + window + ", Range: " + range);
            System.out.println(String.format("%-20s %-10s %-10s %-10s %-10s", "TIME", "AVG", "MIN", "MAX", "COUNT"));
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String now = LocalDateTime.now().format(dtf);

            boolean found = false;
            for (Map.Entry<String, SensorWindow> entry : targetMap.entrySet()) {
                if (entry.getKey().startsWith(name + ":") || entry.getKey().equals(name)) {
                    SensorWindow sw = entry.getValue();
                    System.out.println(String.format("%-20s %-10.1f %-10.1f %-10.1f %-10d", 
                        now, sw.getAvg(), sw.getMin(), sw.getMax(), sw.getCount()));
                    found = true;
                }
            }
            if (!found) System.out.println("No data for sensor in current window.");
        }
    }
}
