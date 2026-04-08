package org.example.command;

import org.example.manager.InputManager;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.utils.RouteBuilder;

public class Insert implements Command {

    private final RouteBuilder routeBuilder;

    public Insert(RouteBuilder routeBuilder){
        this.routeBuilder = routeBuilder;
    }

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.INSERT, arg, routeBuilder.buildRoute());
    }

    @Override
    public String getName() {
        return "insert";
    }

    @Override
    public String getDescription() {
        return "adds new element using a key";
    }
}
