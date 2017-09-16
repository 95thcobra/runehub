package com.runehub.network.session.impl;

import com.runehub.filesystem.buffer.*;
import com.runehub.network.session.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/16/2017
 */
public class HandshakeSession extends Session {
    public HandshakeSession(ChannelHandlerContext ctx, Channel channel) {
        super(ctx, channel);
    }

    @Override
    public void onMessageReceived(ByteBuffer buffer) {
        HandshakeMessage message = HandshakeMessage.getMessage(buffer.readUnsignedByte());
        switch (message) {
            case UPDATE:
                new UpdateSession(ctx, channel).onMessageReceived(buffer);
                break;
            case LOGIN:
            case LOBBY_LOGIN:
                new LoginSession(ctx, channel, message).onMessageReceived(buffer);
                break;
            case UNKNOWN:
                System.err.println("Unknown client stage: " + message.opcode);
                break;
        }
    }

    /**
     * @author Tylurr <tylerjameshurst@gmail.com>
     * @since 9/16/2017
     */
    public enum HandshakeMessage {
        UPDATE(15),
        LOBBY_LOGIN(16),
        LOGIN(14),
        UNKNOWN(-1);
        private  int opcode;

        HandshakeMessage(int opcode) {
            this.opcode = opcode;
        }

        public static HandshakeMessage getMessage(int opcode) {
            for (HandshakeMessage message : values()) {
                if (message.opcode == opcode)
                    return message;
            }
            HandshakeMessage msg = UNKNOWN;
            msg.opcode = opcode;
            return msg;
        }
    }

}
