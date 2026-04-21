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

        if (route == null){
            return new Response(false, "Route must not be null", null);
        }

        collectionManager.insert(Integer.parseInt(arg), route);
        serverCommandExecutor.execute(new Request(CommandType.SAVE_SERVER, null, null));
        return new Response(true, "Element inserted", null);
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
