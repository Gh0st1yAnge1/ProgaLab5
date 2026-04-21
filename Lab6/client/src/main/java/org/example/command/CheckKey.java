package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class CheckKey implements Command{

    @Override
    public Request execute(String args) {
        return new Request(CommandType.CHECK_KEY, args, null);
    }
}
