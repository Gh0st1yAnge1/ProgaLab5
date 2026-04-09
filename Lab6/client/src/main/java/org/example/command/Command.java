package org.example.command;

import org.example.request_and_response.Request;

public interface Command {
    Request execute(String args);
}
