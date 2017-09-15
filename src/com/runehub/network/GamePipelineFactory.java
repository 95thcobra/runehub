package com.runehub.network;

import org.jboss.netty.channel.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public class GamePipelineFactory implements ChannelPipelineFactory {
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("handler", new GameChannelHandler());
        return pipeline;
    }
}
