package org.Gh0st1yAnge1.server_commands;

public class ServerHelp implements ServerCommand{

    public ServerHelp(){}

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "shows available commands";
    }

    @Override
    public String execute(String arg) {
        return "save_with_path <path>/exit/help";
    }
}