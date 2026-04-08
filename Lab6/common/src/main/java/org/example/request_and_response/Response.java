package org.example.request_and_response;

import org.example.model.Route;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;

public record Response(boolean success, String message, LinkedHashMap<Integer, Route> collection)  implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
