package org.example.command;

import org.example.request_and_response.Request;

public interface Command {
    String getName();
    String getDescription();
    Request execute(String args);
}
