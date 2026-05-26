package com.fbp.engine.engine.cli;

import com.fbp.engine.engine.cli.command.*;
import com.fbp.engine.engine.FlowManager;
import picocli.CommandLine;

import java.util.Scanner;

public class FbpCli {
    private final FlowManager flowManager;
    private final CommandLine cmd;

    public FbpCli(FlowManager flowManager) {
        this.flowManager = flowManager;
        this.cmd = createCommandLine();
    }

    private CommandLine createCommandLine() {
        CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.addSubcommand("flow", new FlowCommand(flowManager));
        commandLine.addSubcommand("node", new NodeCommand(flowManager));
        commandLine.addSubcommand("wire", new WireCommand(flowManager));
        commandLine.addSubcommand("sensor", new SensorCommand(flowManager));
        commandLine.addSubcommand("monitor", new MonitorCommand(flowManager));
        commandLine.addSubcommand("stats", new SystemCommand.StatsCommand(flowManager));
        commandLine.addSubcommand("broker", new SystemCommand.BrokerCommand(flowManager));
        commandLine.addSubcommand("influx", new SystemCommand.InfluxCommand(flowManager));
        return commandLine;
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
            String firstArg = args[0].toLowerCase();

            if ("exit".equals(firstArg) || "quit".equals(firstArg)) {
                System.out.println("엔진 CLI를 종료합니다.");
                System.exit(0);
            }

            if ("help".equals(firstArg)) {
                cmd.usage(System.out);
                continue;
            }

            try {
                cmd.execute(args);
            } catch (Exception e) {
                System.out.println("실행 중 에러 발생: " + e.getMessage());
            }
        }
    }
}
