package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class Info implements Command {

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.INFO, arg, null);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "shows info about collection";
    }
}
