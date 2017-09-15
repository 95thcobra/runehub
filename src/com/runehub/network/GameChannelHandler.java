package com.runehub.network;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public final class GameChannelHandler extends SimpleChannelUpstreamHandler {
    private final ChannelGroup channels = new DefaultChannelGroup();

    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> channels.close().awaitUninterruptibly()));
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        channels.add(e.getChannel());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        channels.remove(e.getChannel());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {

    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Object o = ctx.getAttachment();

    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, ExceptionEvent event) throws Exception {
        if (!event.getCause().getMessage().equalsIgnoreCase("An established connection was aborted by the software in your host machine"))
            event.getCause().printStackTrace();
    }

}
