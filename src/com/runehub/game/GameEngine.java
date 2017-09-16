package com.runehub.game;

import com.google.common.base.*;
import com.runehub.game.task.*;
import lombok.*;

import java.util.concurrent.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
@Getter
public class GameEngine extends Thread {
    private final GameWorld world = new GameWorld();
    private final GameTaskManager taskManager = new GameTaskManager(world);

    @Override
    public void run() {
        world.start();
        for (; ; ) {
            Stopwatch watch = Stopwatch.createStarted();
            cycle();
            try {
                Thread.sleep(600 - watch.elapsed(TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void cycle() {
        world.cycle();
        taskManager.cycle();
    }

    public void submit(Runnable task) {
        submit(new GameTask(null, 1, 1, 1) {
            @Override
            public void execute() {
                task.run();
            }
        });
    }

    public void submit(GameTask task) {
        taskManager.getTasks().add(task);
    }

    public void exit() {
        taskManager.getTasks().clear();
    }
}
