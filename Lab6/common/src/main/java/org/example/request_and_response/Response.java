package org.example.request_and_response;

import org.example.model.Route;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record Response(boolean success, String message, List<Route> collection)  implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
