package org.Gh0st1yAnge1.manager;

import org.Gh0st1yAnge1.command.Command;
import org.Gh0st1yAnge1.command.*;
import org.Gh0st1yAnge1.exceptions.InputCancelledException;
import org.Gh0st1yAnge1.request_and_response.CommandType;
import org.Gh0st1yAnge1.request_and_response.Request;
import org.Gh0st1yAnge1.request_and_response.Response;
import org.Gh0st1yAnge1.utils.RouteBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClientCommandManager {

    // Колбэк для отправки запроса и получения ответа — реализуется в ClientApp.
    // Позволяет ClientCommandManager не знать ничего о сети.
    @FunctionalInterface
    public interface RequestSender {
        Response send(Request request) throws IOException, ClassNotFoundException;
    }

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final InputManager inputManager;
    private final RouteBuilder routeBuilder;

    // Стек для защиты от рекурсивных скриптов — хранит canonical path файлов
    private final Set<String> scriptStack = new HashSet<>();

    public ClientCommandManager(InputManager inputManager, RouteBuilder routeBuilder) {
        this.inputManager = inputManager;
        this.routeBuilder = routeBuilder;

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
        commands.put("check_key", new CheckKey());
    }

    /**
     * Разбирает строку ввода и возвращает Request.
     * Не работает с сетью — только формирует запрос.
     *
     * Особые случаи:
     * - execute_script → возвращает Request(EXECUTE_SCRIPT), ClientApp обработает сам
     * - insert → возвращает Request(CHECK_KEY), ClientApp отправит его первым,
     *            а после успешного ответа сам запросит данные и отправит INSERT
     */
    public Request execute(String input) {
        if (input == null || input.trim().isEmpty()) return null;

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];
        String arg = parts.length > 1 ? parts[1] : null;

        Command command = commands.get(commandName);
        if (command == null) {
            System.out.println("Unknown command. Type 'help' to see available commands.");
            return null;
        }

        // execute_script — возвращаем маркер, ClientApp вызовет executeScript()
        if (commandName.equals("execute_script")) {
            return new Request(CommandType.EXECUTE_SCRIPT, arg, null);
        }

        // insert — сначала нужно проверить ключ на сервере.
        // Возвращаем CHECK_KEY; ClientApp знает что после успешного ответа
        // нужно собрать Route и отправить INSERT.
        if (commandName.equals("insert")) {
            return new Request(CommandType.CHECK_KEY, arg, null);
        }

        try {
            return command.execute(arg);
        } catch (InputCancelledException ex) {
            System.out.println("Command cancelled.");
        } catch (Exception e) {
            System.out.println("Error while executing command: " + e.getMessage());
        }
        return null;
    }

    /**
     * Выполняет скрипт из файла построчно.
     * Вся логика проверки рекурсии остаётся здесь.
     * Сетевое взаимодействие делегируется через sender.
     *
     * @param fileName путь к файлу скрипта
     * @param sender   колбэк из ClientApp для синхронной отправки запроса
     */
    public void executeScript(String fileName, RequestSender sender) {
        if (fileName == null || fileName.trim().isEmpty()) {
            System.out.println("Script file name is required.");
            return;
        }

        try {
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("Script file not found: " + fileName);
                return;
            }

            String canonicalPath = file.getCanonicalPath();

            // Проверка рекурсии — тот же файл уже выполняется
            if (scriptStack.contains(canonicalPath)) {
                System.out.println("Recursive script detected, skipping: " + fileName);
                scriptStack.clear();
                return;
            }

            scriptStack.add(canonicalPath);
            Scanner fileScanner = new Scanner(file);
            inputManager.pushScanner(fileScanner);

            System.out.println("Executing script: " + fileName);
            try {
                String line;
                while ((line = inputManager.readline()) != null) {
                    if (line.trim().isEmpty()) continue;
                    System.out.println("> " + line);

                    String[] parts = line.trim().split("\\s+", 2);
                    String commandName = parts[0];
                    String arg = parts.length > 1 ? parts[1] : null;

                    // Рекурсивный вызов execute_script обрабатываем здесь же
                    if (commandName.equals("execute_script")) {
                        executeScript(arg, sender);
                        continue;
                    }

                    // insert внутри скрипта: сначала CHECK_KEY, затем INSERT
                    if (commandName.equals("insert")) {
                        Request checkRequest = new Request(CommandType.CHECK_KEY, arg, null);
                        Response checkResponse = sender.send(checkRequest);
                        if (checkResponse == null) {
                            System.out.println("Server disconnected during script.");
                            return;
                        }
                        if (checkResponse.success()) {
                            System.out.println(checkResponse.message());
                            try {
                                Request insertRequest = new Insert(routeBuilder).execute(arg);
                                Response insertResponse = sender.send(insertRequest);
                                if (insertResponse == null) {
                                    System.out.println("Server disconnected during script.");
                                    return;
                                }
                                if (insertResponse.message() != null)
                                    System.out.println(insertResponse.message());
                            } catch (InputCancelledException e) {
                                System.out.println("Route building cancelled.");
                            }
                        } else {
                            System.out.println(checkResponse.message());
                        }
                        continue;
                    }

                    // Все остальные команды
                    Request request = execute(line);
                    if (request == null) continue;

                    Response response = sender.send(request);
                    if (response == null) {
                        System.out.println("Server disconnected during script.");
                        return;
                    }
                    if (response.message() != null) System.out.println(response.message());
                    if (response.collection() != null) System.out.println(response.collection());
                }
            } finally {
                inputManager.popScanner();
                fileScanner.close();
                scriptStack.remove(canonicalPath);
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error while executing script: " + e.getMessage());
        }
    }

    public Map<String, Command> getCommands() {
        return commands;
    }
}
