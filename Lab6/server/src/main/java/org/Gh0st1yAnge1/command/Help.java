package org.Gh0st1yAnge1.command;

import org.Gh0st1yAnge1.manager.ServerCommandExecutor;
import org.Gh0st1yAnge1.model.Route;
import org.Gh0st1yAnge1.request_and_response.Response;

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

        String answer = "Available commands:\n\n--execute_script--\nexecutes script\n";
        for (Command command: serverCommandExecutor.getCommands().values()){
            if (command.getName().equals("save")){
                continue;
            }
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
