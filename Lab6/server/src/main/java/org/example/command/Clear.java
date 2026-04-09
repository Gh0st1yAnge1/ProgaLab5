package org.example.command;

import org.example.manager.CollectionManager;
import org.example.model.Route;
import org.example.request_and_response.Response;
import org.example.utils.IdGenerator;

public class Clear implements Command{

    private final CollectionManager collectionManager;

    public Clear(CollectionManager collectionManager){
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(String arg, Route route) {

        if (arg != null){
            return new Response(false, "Usage: clear", null);
        }

        collectionManager.clear();
        IdGenerator.reset();
        return new Response(true, "Successfully cleared!", null);
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "clears collection";
    }
}
