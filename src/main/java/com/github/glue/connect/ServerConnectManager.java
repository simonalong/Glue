package com.github.glue.connect;

import com.github.glue.ChannelHelper;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.glue.GlueConstant.LOG_PRE;

/**
 * 服务端的链接管理器
 *
 * @author shizi
 * @since 2020/3/12 下午9:19
 */
@Slf4j
public class ServerConnectManager implements ConnectManager {

    private Map<String, ServerNettyConnector> channelMap = new ConcurrentHashMap<>();

    @Override
    public void addConnect(Connector connector) {
        Channel channel = connector.getChannel();
        String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        channelMap.computeIfAbsent(addr, address -> new ServerNettyConnector(channel, address));
    }

    public void addConnect(Channel channel){
        final String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if (channelMap.containsKey(addr)) {
            return;
        }

        channelMap.put(addr, new ServerNettyConnector(channel, addr));
    }

    @Override
    public void closeConnect(Channel channel) {
        final String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if (!channelMap.containsKey(addr)) {
            return;
        }

        try {
            channel.close().addListener(future -> log.info(LOG_PRE + "closeChannel: close the connection to remote address[{}] result: {}", addr, future.isSuccess()));
        } finally {
            channelMap.remove(addr);
        }
    }

    public ServerNettyConnector getConnector(String addr) {
        if (channelMap.containsKey(addr)) {
            return null;
        }

        return channelMap.get(addr);
    }

    public Collection<ServerNettyConnector> getAllConnector(){
        return channelMap.values();
    }
}
