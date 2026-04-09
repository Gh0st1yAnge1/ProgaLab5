package org.example.command;

import org.example.manager.InputManager;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.utils.RouteBuilder;

public class Update implements Command {

    private final RouteBuilder routeBuilder;

    public Update(RouteBuilder routeBuilder){
        this.routeBuilder = routeBuilder;
    }

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.UPDATE, arg, routeBuilder.buildRoute());
    }
}
