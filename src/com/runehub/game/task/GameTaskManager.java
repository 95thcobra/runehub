package com.runehub.game.task;

import com.runehub.game.*;
import lombok.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
@Getter
public class GameTaskManager {
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Queue<GameTask> tasks = new LinkedList<>();
    private final GameWorld world;

    public GameTaskManager(GameWorld world) {
        this.world = world;
    }

    public void cycle() {
        GameTask task;
        LinkedList<GameTask> continued = new LinkedList<>();
        while ((task = tasks.poll()) != null) {
            if (task.getPlayer() != null) {
                if (task.getPlayer().getWorld() == null)
                    task.setCancelled(true);
            }
            task.cycle();
            if (!task.isCancelled())
                continued.add(task);
        }
        tasks.addAll(continued);
    }

}
