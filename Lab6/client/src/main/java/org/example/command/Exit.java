package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class Exit implements Command {

    @Override
    public Request execute(String args) {
        return new Request(CommandType.EXIT, args, null);
    }

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String getDescription() {
        return "terminates program.";
    }
}
