package org.example.command;

import org.example.manager.CollectionManager;
import org.example.model.Route;
import org.example.request_and_response.Response;

public class Show implements Command {

    private final CollectionManager collectionManager;

    public Show(CollectionManager collectionManager){
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(String arg, Route route) {

        if (arg == null || arg.isBlank()){
            return new Response(false, "Usage: show", null);
        }

        return new Response(true, "All available routes:\n", collectionManager.showCollection());
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public String getDescription() {
        return "shows all collection elements";
    }
}
