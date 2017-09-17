package com.runehub.network.session.impl;

import com.runehub.filesystem.*;
import com.runehub.filesystem.buffer.*;
import com.runehub.network.session.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public class GameSession extends Session {
    protected GameSession(FileSystem fileSystem, ChannelHandlerContext ctx, Channel channel) {
        super(fileSystem, ctx, channel);
    }

    @Override
    public void onMessageReceived(ByteBuffer buffer) {
    }
}
