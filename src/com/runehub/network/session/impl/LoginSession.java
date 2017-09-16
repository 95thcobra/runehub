package com.runehub.network.session.impl;

import com.runehub.filesystem.buffer.*;
import com.runehub.network.session.*;
import com.runehub.network.session.impl.HandshakeSession.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public class LoginSession extends Session {
    protected LoginSession(ChannelHandlerContext ctx,Channel channel, HandshakeMessage message) {
        super(ctx, channel);
    }

    @Override
    public void onMessageReceived(ByteBuffer buffer) {
    }
}
