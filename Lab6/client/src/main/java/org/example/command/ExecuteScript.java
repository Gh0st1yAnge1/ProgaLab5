package org.example.command;


import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class ExecuteScript implements Command{

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.EXECUTE_SCRIPT, arg, null);
    }

    @Override
    public String getName() {
        return "execute_script";
    }

    @Override
    public String getDescription() {
        return "executes script";
    }
}
