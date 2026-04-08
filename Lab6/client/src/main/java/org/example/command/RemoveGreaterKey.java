package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class RemoveGreaterKey implements Command {

    @Override
    public Request execute(String args) {
        return new Request(CommandType.REMOVE_GREATER_KEY, args, null);
    }

    @Override
    public String getName() {
        return "remove_greater_key";
    }

    @Override
    public String getDescription() {
        return "removes all collection elements,\nwhich key is more than inserted value";
    }
}
