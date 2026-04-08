package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class CountByDistance implements Command{

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.COUNT_BY_DISTANCE, arg, null);
    }

    @Override
    public String getName() {
        return "count_by_distance";
    }

    @Override
    public String getDescription() {
        return "shows number of elements, which distance fields\nare equals to inserted value";
    }
}
