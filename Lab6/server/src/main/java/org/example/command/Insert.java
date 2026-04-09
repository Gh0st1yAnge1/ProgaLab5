package org.example.command;

import org.example.manager.CollectionManager;
import org.example.manager.ServerCommandExecutor;
import org.example.model.Route;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;

public class Insert implements Command {
    private final CollectionManager collectionManager;
    private final ServerCommandExecutor serverCommandExecutor;

    public Insert(CollectionManager collectionManager, ServerCommandExecutor serverCommandExecutor) {
        this.collectionManager = collectionManager;
        this.serverCommandExecutor = serverCommandExecutor;
    }

    @Override
    public Response execute(String arg, Route route) {

        if (arg == null || arg.isBlank()){
            return new Response(false, "Usage: insert <key>", null);
        }

        if (route == null){
            return new Response(false, "Route must not be null", null);
        }

        int key;
        try {
            key = Integer.parseInt(arg);
        } catch (NumberFormatException ex){
            return new Response(false, "Key must be integer.", null);
        }

        boolean isInserted = collectionManager.insert(key, route);

        if (isInserted){
            serverCommandExecutor.execute(new Request(CommandType.SAVE_SERVER, null, null));
            return new Response(true, "Element inserted", null);
        } else {
            return new Response(false, "Key already exists", null);
        }
    }

    @Override
    public String getName() {
        return "insert";
    }

    @Override
    public String getDescription() {
        return "adds new element using a key";
    }
}
