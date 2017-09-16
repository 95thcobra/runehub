package com.runehub.game.task;

import com.runehub.game.model.entity.player.*;
import lombok.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
@Getter
@Setter
public abstract class GameTask {
    private final long delay;
    private final long initialDelay;
    private final int maxLifepoints;
    private int lifepoints;
    private boolean cancelled;
    private long countdown;
    private Player player;

    public GameTask(long delay) {
        this(null, delay, 1, -1);
    }

    public GameTask(long delay, long initialDelay) {
        this(null, delay, initialDelay, -1);
    }

    public GameTask(Player player, long delay) {
        this(player, delay, 1, -1);
    }

    public GameTask(Player player, long delay, long initialDelay) {
        this(player, delay, initialDelay, -1);
    }

    public GameTask(Player player, long delay, long initialDelay, int maxLifepoints) {
        this.delay = delay;
        this.initialDelay = initialDelay;
        this.maxLifepoints = maxLifepoints;
        this.lifepoints = maxLifepoints;
        this.countdown = initialDelay;
        this.player = player;
    }

    public void cycle() {
        if (cancelled)
            return;
        if (countdown <= 0) {
            if (lifepoints == 0) {
                cancelled = true;
                return;
            }
            execute();
            countdown = delay;
            lifepoints--;
        }
        countdown--;

    }

    public abstract void execute();

}
