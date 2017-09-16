package com.runehub;

import com.runehub.filesystem.*;
import com.runehub.game.*;
import com.runehub.network.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
public class RuneHub {
    private final FileSystem fileSystem = FileSystem.open("./resources/cache/");
    private final GameEngine engine = new GameEngine();
    private final GameServer server = new GameServer();

    private void start() {
        engine.start();
        server.bind(43594);
    }

    private void exit() {
        engine.exit();
        server.exit();
    }

    public static void main(String[] args) {
        RuneHub hub = new RuneHub();
        hub.start();
        Runtime.getRuntime().addShutdownHook(new Thread(hub::exit));
    }
}
