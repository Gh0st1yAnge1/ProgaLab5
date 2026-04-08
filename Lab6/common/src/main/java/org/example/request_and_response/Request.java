package org.example.request_and_response;

import org.example.model.Route;

import java.io.Serial;
import java.io.Serializable;

public record Request(CommandType commandType, String argument, Route route) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
