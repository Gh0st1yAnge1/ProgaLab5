package org.example.command;

import org.example.manager.CollectionManager;
import org.example.model.Route;
import org.example.request_and_response.Response;

public class CheckKey implements Command{
    private final CollectionManager collectionManager;

    public CheckKey(CollectionManager collectionManager){
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(String arg, Route route) {

        if (arg == null || arg.isBlank()){
            return new Response(false, "Usage: insert <key>", null);
        }

        int key;
        try {
            key = Integer.parseInt(arg);
        } catch (NumberFormatException ex){
            return new Response(false, "Key must be integer.", null);
        }

        if (collectionManager.checkKey(key)){
            return new Response(false, "Key already exists.", null);
        } else {
            return new Response(true, "Key is available!", null);
        }
    }

    @Override
    public String getName() {
        return "check_key";
    }

    @Override
    public String getDescription() {
        return "checks_key";
    }
}
