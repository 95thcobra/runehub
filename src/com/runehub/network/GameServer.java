package com.runehub.network;

import com.runehub.filesystem.*;
import org.jboss.netty.bootstrap.*;
import org.jboss.netty.channel.socket.nio.*;

import java.net.*;
import java.util.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/14/2017
 */
public class GameServer {
    private final ServerBootstrap bootstrap = new ServerBootstrap();

    public GameServer(FileSystem fileSystem) {
        bootstrap.setFactory(new NioServerSocketChannelFactory());
        bootstrap.setPipelineFactory(new GamePipelineFactory(fileSystem));
        bootstrap.setOptions(new HashMap<String, Object>() {{
            put("reuseAddress", true);
            put("child.tcpNoDelay", true);
            put("child.TcpAckFrequency", true);
            put("child.keepAlive", true);
        }});

    }

    public void bind(int port) {
        bootstrap.bind(new InetSocketAddress(port));
    }

    public void exit() {
        bootstrap.releaseExternalResources();
    }
}
