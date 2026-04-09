package org.example.manager;

import org.example.ClientApp;
import org.example.command.Command;

import java.io.File;
import java.nio.channels.SocketChannel;
import java.util.*;
import org.example.command.*;
import org.example.exceptions.InputCancelledException;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;
import org.example.utils.RouteBuilder;

public class ClientCommandManager {
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final InputManager inputManager;
    private final Set<String> scriptStack = new HashSet<>();

    public ClientCommandManager(InputManager inputManager) {
        this.inputManager = inputManager;
        RouteBuilder routeBuilder = new RouteBuilder(inputManager);

        commands.put("average_of_distance", new AverageOfDistance());
        commands.put("help", new Help());
        commands.put("exit", new Exit());
        commands.put("info", new Info());
        commands.put("show", new Show());
        commands.put("clear", new Clear());
        commands.put("remove_key", new RemoveKey());
        commands.put("remove_greater_key", new RemoveGreaterKey());
        commands.put("count_by_distance", new CountByDistance());
        commands.put("filter_less_than_distance", new FilterLessThanDistance());
        commands.put("execute_script", new ExecuteScript());
        commands.put("insert", new Insert(routeBuilder));
        commands.put("update", new Update(routeBuilder));
        commands.put("replace_if_lower", new ReplaceIfLower(routeBuilder));
        commands.put("remove_greater", new RemoveGreater(routeBuilder));
    }

    public Request execute(String input, SocketChannel socketChannel){

        if (input == null || input.trim().isEmpty()){
            return null;
        }

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];
        String arg = parts.length > 1 ? parts[1] : null;

        Command command = commands.get(commandName);

        if (command == null){
            System.out.println("Unknown command. Type 'help' to see available commands.");
            return null;
        }

        if (commandName.equals("execute_script")){
            executeScript(arg, socketChannel);
        }

        try{
            return command.execute(arg);
        } catch (InputCancelledException ex) {
            System.out.println("Command cancelled.");
        } catch (Exception e) {
            System.out.println("Error while executing command " + e.getMessage());
        }
        return null;
    }

    public void executeScript(String fileName, SocketChannel socketChannel) {


        try{
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("Script file not found.");
                return;
            }

            String canonicalPath = file.getCanonicalPath();

            if (scriptStack.contains(canonicalPath)){
                System.out.println("Recursive detected");
                scriptStack.clear();
                return;
            }

            scriptStack.add(canonicalPath);
            Scanner fileScanner = new Scanner(file);
            inputManager.pushScanner(fileScanner);

            System.out.println("Executing script..." + fileName);
            try {
                while (true) {
                    String line = inputManager.readline();
                    if (line == null) break;
                    if (line.trim().isEmpty()) continue;
                    System.out.println("> " + line);

                    Request request = execute(line, socketChannel);

                    if (request == null){
                        continue;
                    }

                    if (request.commandType() == CommandType.EXECUTE_SCRIPT){
                        executeScript(request.argument(), socketChannel);
                        continue ;
                    }

                    ClientApp.sendRequest(socketChannel, request);

                    Response response = ClientApp.readResponse(socketChannel);

                    if (response.message() != null) {
                        System.out.println(response.message());
                        if (response.collection() != null) {
                            System.out.println(response.collection());
                        }
                    } else {
                        System.out.println("Server disconnected during script execution.");
                        return;
                    }
                }

            } finally {
                inputManager.popScanner();
                fileScanner.close();
            }
        } catch (Exception e) {
            System.out.println("Error while executing script: " + e.getMessage());
        }
    }

    public Map<String, Command> getCommands(){
        return commands;
    }
}

