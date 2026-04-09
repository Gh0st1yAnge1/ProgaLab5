package org.example.manager;

import org.example.command.*;
import org.example.request_and_response.Request;

import java.util.*;

public class ServerCommandExecutor {

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public ServerCommandExecutor(){
        String filePath = System.getenv("FILE_NAME");
        FileManager fileManager = new FileManager(filePath);
        CollectionManager collectionManager = new CollectionManager();
        commands.put("average_of_distance", new AverageOfDistance(collectionManager));
        commands.put("help", new Help(this));
        commands.put("info", new Info(collectionManager));
        commands.put("show", new Show(collectionManager));
        commands.put("clear", new Clear(collectionManager));
        commands.put("remove_key", new RemoveKey(collectionManager, this));
        commands.put("remove_greater_key", new RemoveGreaterKey(collectionManager, this));
        commands.put("count_by_distance", new CountByDistance(collectionManager));
        commands.put("filter_less_than_distance", new FilterLessThanDistance(collectionManager));
        commands.put("insert", new Insert(collectionManager, this));
        commands.put("update", new Update(collectionManager, this));
        commands.put("replace_if_lower", new ReplaceIfLower(collectionManager, this));
        commands.put("remove_greater", new RemoveGreater(collectionManager, this));
        commands.put("save", new Save(fileManager,collectionManager));
    }

    public void execute(Request request){
        String name = request.commandType().toString().toLowerCase();
        Command command = commands.get(name);
        command.execute(request.argument(), request.route());
    }

    public Map<String, Command> getCommands(){
        return commands;
    }
}
