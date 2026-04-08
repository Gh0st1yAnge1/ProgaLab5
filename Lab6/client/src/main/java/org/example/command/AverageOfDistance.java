package org.example.command;

import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;

public class AverageOfDistance implements Command{

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.AVERAGE_OF_DISTANCE, arg, null);
    }

    @Override
    public String getName() {
        return "average_of_distance";
    }

    @Override
    public String getDescription() {
        return "gives you average of distance fields in collection";
    }
}
