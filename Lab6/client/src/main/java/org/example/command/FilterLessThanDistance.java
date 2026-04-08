package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class FilterLessThanDistance implements Command {

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.FILTER_LESS_THAN_DISTANCE, arg, null);
    }

    @Override
    public String getName() {
        return "filter_less_than_distance";
    }

    @Override
    public String getDescription() {
        return "shows elements, which distance field\nis less than inserted value";
    }
}
