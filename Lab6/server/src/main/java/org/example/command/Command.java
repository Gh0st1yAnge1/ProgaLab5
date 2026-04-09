package org.example.command;

import org.example.model.Route;
import org.example.request_and_response.Response;

public interface Command {
    String getName();
    String getDescription();
    Response execute(String arg, Route route);
}
