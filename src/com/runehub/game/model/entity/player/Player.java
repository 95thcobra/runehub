package com.runehub.game.model.entity.player;

import com.runehub.game.*;
import com.runehub.game.model.entity.*;
import lombok.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
@Getter
public class Player extends Entity {
    private final String username;
    private transient GameWorld world;

    public Player(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Player))
            return false;
        Player player = (Player) o;
        return getUsername().equals(player.getUsername());
    }

    @Override
    public int hashCode() {
        return getUsername().hashCode();
    }
}
