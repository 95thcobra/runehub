package com.runehub.network.session;

import com.runehub.filesystem.*;
import com.runehub.filesystem.buffer.*;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public abstract class Session {
    protected final FileSystem fileSystem;
    protected final Channel channel;
    protected final ChannelHandlerContext ctx;

    protected Session(FileSystem fileSystem, ChannelHandlerContext ctx, Channel channel) {
        this.fileSystem = fileSystem;
        this.channel = channel;
        this.ctx = ctx;
        ctx.setAttachment(this);
    }

    public abstract void onMessageReceived(ByteBuffer buffer);

    public void onSessionExit() {
        if (channel.isOpen())
            channel.close();
    }

    public final ChannelFuture write(ChannelBuffer buffer) {
        if (buffer == null || !channel.isConnected())
            return null;
        synchronized (channel) {
            return channel.write(buffer);
        }
    }

    public final ChannelFuture write(ByteBuffer buf) {
        return write(ChannelBuffers.copiedBuffer(buf.getBuffer(), 0, buf.getOffset()));
    }


}
