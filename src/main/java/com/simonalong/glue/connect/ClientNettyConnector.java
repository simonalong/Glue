package com.simonalong.glue.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * @author shizi
 * @since 2020/3/4 下午7:03
 */
public class ClientNettyConnector extends AbstractConnectSender{

    private final ChannelFuture channelFuture;
    private final String address;

    public ClientNettyConnector(ChannelFuture channelFuture, String address) {
        this.channelFuture = channelFuture;
        this.address = address;
    }

    @Override
    public Channel getChannel() {
        return this.channelFuture.channel();
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public boolean isOK() {
        return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }
}
