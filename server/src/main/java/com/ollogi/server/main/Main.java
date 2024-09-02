package com.ollogi.server.main;

import com.general.command.Command;
import com.general.io.Interrogator;
import com.general.managers.CollectionManager;
import com.general.managers.CommandManager;
import com.general.models.Flat;
import com.general.network.Request;
import com.general.network.Response;
import com.ollogi.server.commands.*;
import com.ollogi.server.data.FlatDAO;
import com.ollogi.server.data.UserDAO;
import com.ollogi.server.managers.FlatCollectionManager;
import com.ollogi.server.network.Handler;
import com.ollogi.server.network.TCPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

import static com.ollogi.server.managers.DatabaseManager.createDatabaseIfNotExists;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PORT = 28374;

    public static void main(String[] args) {
        Interrogator.setUserScanner(new Scanner(System.in));
        createDatabaseIfNotExists();
        CollectionManager<Flat> collectionManager = initializeCollectionManager();

        startConsoleListener(collectionManager);

        CommandManager commandManager = initializeCommandManager(collectionManager);
        startServer(commandManager);
    }

    private static CollectionManager<Flat> initializeCollectionManager() {
        return new FlatCollectionManager(new FlatDAO(), new UserDAO());
    }

    private static CommandManager initializeCommandManager(CollectionManager<Flat> collectionManager) {
        CommandManager commandManager = new CommandManager();
        UserDAO userDAO = new UserDAO();
        Handler.setUserDAO(userDAO);
        initCommands(collectionManager, commandManager, userDAO);
        return commandManager;
    }

    private static void startServer(CommandManager commandManager) {
        Handler.setCommandManager(commandManager);
        new TCPServer(PORT).start();
    }

    public static void initCommands(CollectionManager<Flat> collectionManager, CommandManager commandManager, UserDAO userDAO) {
        commandManager.register("help", new Help(commandManager));
        commandManager.register("info", new Info(collectionManager));
        commandManager.register("show", new Show<>(collectionManager));
        commandManager.register("add", new Add<>(collectionManager));
        commandManager.register("update", new Update<>(collectionManager));
        commandManager.register("remove_by_id", new RemoveById<>(collectionManager));
        commandManager.register("clear", new Clear(collectionManager));
        commandManager.register("remove_greater", new RemoveGreater<>(collectionManager));
        commandManager.register("remove_lower", new RemoveLower<>(collectionManager));
        commandManager.register("add_if_min", new AddIfMin<>(collectionManager));
        commandManager.register("sum_of_height", new SumOfHeight(collectionManager));
        commandManager.register("group_counting_by_house", new GroupCountingByHouse(collectionManager));
        commandManager.register("filter_starts_with_name", new FilterStartsWithName(collectionManager));
        commandManager.register("register", new Register(userDAO));
        commandManager.register("login", new Login(userDAO));
        Command executeScriptCommand = new Command("execute_script", "исполнить скрипт из указанного файла") {

            public Request execute(String[] arguments) {
                return null; // Stub implementation
            }

            @Override
            public Response execute(Request request) {
                return null; // Stub implementation
            }
        };
        commandManager.register("execute_script", executeScriptCommand);

    }


    private static void startConsoleListener(CollectionManager<Flat> collectionManager) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine().trim();
                if ("exit".equalsIgnoreCase(input)) {
                    logger.info("Завершение работы программы...");
                    System.exit(0);
                } else {
                    logger.warn("Неизвестная команда: {}", input);
                }
            }
        }).start();
    }
}
