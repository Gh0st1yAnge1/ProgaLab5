package org.example.command;

import org.example.manager.ServerCommandExecutor;
import org.example.model.Route;
import org.example.request_and_response.Response;

public class Help implements Command {

    private final ServerCommandExecutor serverCommandExecutor;

    public Help(ServerCommandExecutor serverCommandExecutor){
        this.serverCommandExecutor = serverCommandExecutor;
    }


    @Override
    public Response execute(String arg, Route route) {

        if (arg != null){
            return new Response(false, "Usage: help", null);
        }

        String answer = "";
        answer += "Available commands:\n";
        for (Command command: serverCommandExecutor.getCommands().values()){
            answer += " \n";
            answer += "--" + command.getName() + "--\n";
            answer += command.getDescription()+ "\n";
        }

        return new Response(true, answer, null);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "shows available commands";
    }
}
