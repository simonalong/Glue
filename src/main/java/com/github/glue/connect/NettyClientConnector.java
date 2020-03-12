package com.github.glue.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * @author shizi
 * @since 2020/3/4 下午7:03
 */
public class NettyClientConnector extends AbstractConnectSender{

    private final ChannelFuture channelFuture;
    private String addr;

    public NettyClientConnector(ChannelFuture channelFuture, String addr) {
        this.channelFuture = channelFuture;
        this.addr = addr;
    }

    @Override
    public Channel getChannel() {
        return this.channelFuture.channel();
    }

    @Override
    public String getAddr() {
        return addr;
    }

    @Override
    public boolean isOK() {
        return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }
}
