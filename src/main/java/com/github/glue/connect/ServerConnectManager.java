package com.github.glue.connect;

import com.github.glue.ChannelHelper;
import com.github.glue.NettyServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端的链接管理器
 *
 * @author shizi
 * @since 2020/3/12 下午9:19
 */
@Slf4j
public class ServerConnectManager implements ConnectManager {

    private Map<String, NettyServerConnector> channelMap = new ConcurrentHashMap<>();

    @Override
    public void addConnect(Connector connector) {
        Channel channel = connector.getChannel();
        String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        channelMap.computeIfAbsent(addr, address -> new NettyServerConnector(channel, address));
    }

    public void addConnect(Channel channel){
        final String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if (channelMap.containsKey(addr)) {
            return;
        }

        channelMap.put(addr, new NettyServerConnector(channel, addr));
    }

    @Override
    public void closeConnect(Channel channel) {
        final String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if (!channelMap.containsKey(addr)) {
            return;
        }

        try {
            channel.close().addListener(future -> log.info("closeChannel: close the connection to remote address[{}] result: {}", addr, future.isSuccess()));
        } finally {
            channelMap.remove(addr);
        }
    }

    public NettyServerConnector getConnector(String addr) {
        if (channelMap.containsKey(addr)) {
            return null;
        }

        return channelMap.get(addr);
    }

    public Collection<NettyServerConnector> getAllConnector(){
        return channelMap.values();
    }
}
