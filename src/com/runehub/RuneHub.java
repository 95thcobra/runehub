package com.runehub;

import com.runehub.game.*;
import com.runehub.game.task.*;
import com.runehub.game.util.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
public class RuneHub {
    private final GameEngine engine = new GameEngine();

    private void start() {
        engine.start();
        engine.submit(new GameTask(TickUnit.MINUTES.toGameTicks(5)) {
            @Override
            public void execute() {
                System.out.println("Spam");
            }
        });
    }

    private void exit() {
        engine.exit();
    }

    public static void main(String[] args) {
        RuneHub hub = new RuneHub();
        hub.start();
        Runtime.getRuntime().addShutdownHook(new Thread(hub::exit));
    }
}
