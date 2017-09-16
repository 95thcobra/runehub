package com.runehub.network;

import com.runehub.filesystem.buffer.*;
import com.runehub.network.session.*;
import com.runehub.network.session.impl.*;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public final class GameChannelHandler extends SimpleChannelUpstreamHandler {
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.setAttachment(new HandshakeSession(ctx, e.getChannel()));
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Session session = getSession(ctx);
        if (session != null)
            session.onSessionExit();
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Session session = getSession(ctx);
        if (session != null)
            session.onSessionExit();

    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if (!(e.getMessage() instanceof ChannelBuffer))
            return;
        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        Session session = getSession(ctx);
        if (session != null) {
            buffer.markReaderIndex();
            int avail = buffer.readableBytes();
            if (avail < 1 || avail > 7500)
                return;
            byte[] bytes = new byte[avail];
            buffer.readBytes(bytes);
            session.onMessageReceived(new ByteBuffer(bytes));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event) throws Exception {
        if (!event.getCause().getMessage().equalsIgnoreCase("An existing connection was forcibly closed by the remote host") && !event.getCause()
                .getMessage().equalsIgnoreCase("An established connection was aborted by the software in your host machine")) {
            event.getCause().printStackTrace();
        }
        ctx.getChannel().close();
    }

    private Session getSession(ChannelHandlerContext ctx) {
        if (ctx.getAttachment() instanceof Session)
            return (Session) ctx.getAttachment();
        else
            return null;
    }

}
