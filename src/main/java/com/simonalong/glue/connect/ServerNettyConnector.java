package com.simonalong.glue.connect;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shizi
 * @since 2020/3/12 下午9:40
 */
@Slf4j
public class ServerNettyConnector extends AbstractConnectSender {

    private final Channel channel;
    private final String address;

    public ServerNettyConnector(Channel channel, String address) {
        this.channel = channel;
        this.address = address;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public boolean isOK() {
        return channel != null && channel.isActive();
    }
}
