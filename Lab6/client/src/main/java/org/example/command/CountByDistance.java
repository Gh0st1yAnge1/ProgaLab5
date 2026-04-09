package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class CountByDistance implements Command{

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.COUNT_BY_DISTANCE, arg, null);
    }
}
