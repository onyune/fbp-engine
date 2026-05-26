package com.fbp.engine.engine.cli.command;

import picocli.CommandLine.Command;

@Command(name = "fbp", 
         mixinStandardHelpOptions = true, 
         version = "FBP Engine CLI 1.0",
         description = "FBP IoT Rule Engine CLI")
public class RootCommand implements Runnable {
    public RootCommand() {}
    @Override
    public void run() {
        // Root command with no options just shows help
        new picocli.CommandLine(this).usage(System.out);
    }
}
