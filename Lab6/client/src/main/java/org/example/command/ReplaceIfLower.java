package org.example.command;

import org.example.manager.InputManager;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.utils.RouteBuilder;

public class ReplaceIfLower implements Command {

    private final RouteBuilder routeBuilder;

    public ReplaceIfLower(RouteBuilder routeBuilder){
        this.routeBuilder = routeBuilder;
    }

    @Override
    public Request execute(String arg) {
        return new Request(CommandType.REPLACE_IF_LOWER, arg, routeBuilder.buildRoute());
    }

    @Override
    public String getName() {
        return "replace_if_lower";
    }

    @Override
    public String getDescription() {
        return "replaces element using a key,\nif new value is less than old";
    }
}
