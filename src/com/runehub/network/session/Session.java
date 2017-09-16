package com.runehub.network.session;

import com.runehub.filesystem.buffer.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public abstract class Session {
    protected final Channel channel;
    protected final ChannelHandlerContext ctx;

    protected Session(ChannelHandlerContext ctx, Channel channel) {
        this.channel = channel;
        this.ctx = ctx;
        ctx.setAttachment(this);
    }

    public abstract void onMessageReceived(ByteBuffer buffer);

    public void onSessionExit() {
        if (channel.isOpen())
            channel.close();
    }
}
