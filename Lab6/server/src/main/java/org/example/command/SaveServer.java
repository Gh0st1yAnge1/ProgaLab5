package org.example.command;

import org.example.manager.CollectionManager;
import org.example.manager.FileManager;
import org.example.model.Route;
import org.example.request_and_response.Response;

import java.util.LinkedHashMap;

public class SaveServer implements Command {

    private final FileManager fileManager;
    private final CollectionManager collectionManager;

    public SaveServer(FileManager fileManager, CollectionManager collectionManager){
        this.fileManager = fileManager;
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(String arg, Route route) {

        String answer = fileManager.saveCollection(new LinkedHashMap<>(collectionManager.show()));
        if (answer.equals("File path is not found.")){
            return new Response(false, "File path is not found.", null);
        }

        if (answer.equals("Error writing file.")){
            return new Response(false, "Error writing file.", null);
        }

        return new Response(true, answer, null);
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "saves collection to file";
    }
}
