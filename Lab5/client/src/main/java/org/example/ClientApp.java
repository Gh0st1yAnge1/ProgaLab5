package org.example;

import org.example.manager.*;
import org.example.utils.Runner;
import java.io.IOException;

public class ClientApp {
    public static void main(String[] args) throws IOException {

        TerminalManager.enableRawMode();
        Runtime.getRuntime().addShutdownHook(
                new Thread(TerminalManager::disableRawMode)
        );
        Runner.run();
    }
}