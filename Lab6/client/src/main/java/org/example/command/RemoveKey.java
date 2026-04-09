package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class RemoveKey implements Command {

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.REMOVE_KEY, arg, null);
    }
}
