package com.runehub.network.session.impl;

import com.runehub.filesystem.*;
import com.runehub.filesystem.buffer.*;
import com.runehub.network.session.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/16/2017
 */
public class HandshakeSession extends Session {
    public HandshakeSession(FileSystem fileSystem, ChannelHandlerContext ctx, Channel channel) {
        super(fileSystem, ctx, channel);
    }

    @Override
    public void onMessageReceived(ByteBuffer buffer) {
        HandshakeMessage message = HandshakeMessage.getMessage(buffer.readUnsignedByte());
        System.out.println(message);
        switch (message) {
            case UPDATE:
                int size = buffer.readUnsignedByte();
                //
                int build = buffer.readInt();
                int subBuild = buffer.readInt();
                String key = buffer.readString();
                System.out.println(key);
                UpdateSession session = new UpdateSession(fileSystem, ctx, channel);
                session.startup();
                System.out.println(buffer.getRemaining());
                break;
            case LOGIN:
            case LOBBY_LOGIN:
                new LoginSession(fileSystem, ctx, channel, message).onMessageReceived(buffer);
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
        private int opcode;

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
