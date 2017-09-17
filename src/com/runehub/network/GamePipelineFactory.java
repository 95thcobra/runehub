package com.runehub.network;

import com.runehub.filesystem.*;
import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public class GamePipelineFactory implements ChannelPipelineFactory {
    private final FileSystem fileSystem;

    public GamePipelineFactory(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("handler", new GameChannelHandler(fileSystem));
        return pipeline;
    }
}
