package org.Gh0st1yAnge1.manager;

import org.Gh0st1yAnge1.audit.AuditProducer;
import org.Gh0st1yAnge1.command.*;
import org.Gh0st1yAnge1.request_and_response.CommandType;
import org.Gh0st1yAnge1.request_and_response.Request;
import org.Gh0st1yAnge1.request_and_response.Response;

import java.util.*;

public class ServerCommandExecutor {

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final AuditProducer auditProducer;
    private static final Set<CommandType> AUDITED_COMMANDS = Set.of(
            CommandType.INSERT,
            CommandType.UPDATE,
            CommandType.REMOVE_KEY,
            CommandType.REMOVE_GREATER_KEY,
            CommandType.REMOVE_GREATER,
            CommandType.REPLACE_IF_LOWER,
            CommandType.CLEAR,
            CommandType.SHOW
    );


    public ServerCommandExecutor(CollectionManager collectionManager, FileManager fileManager, AuditProducer auditProducer){
        this.auditProducer = auditProducer;
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
        commands.put("save_server", new SaveServer(fileManager,collectionManager));
        commands.put("check_key", new CheckKey(collectionManager));
    }

    public Response execute(Request request){
        String name = request.commandType().toString().toLowerCase();
        Command command = commands.get(name);
        if (command == null){
            return new Response(false, "Unknown command. Type 'help' to see available commands", null);
        }

        Response response = command.execute(request.argument(), request.route());

        if (auditProducer != null && AUDITED_COMMANDS.contains(request.commandType())) {
            auditProducer.sendIfAuditable(
                    request.commandType().name(),
                    request.argument(),
                    response.success(),
                    response.message()
            );
        }

        return response;
    }

    public Map<String, Command> getCommands(){
        return commands;
    }
}
