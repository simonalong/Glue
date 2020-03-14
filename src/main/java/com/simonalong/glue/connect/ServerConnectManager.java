package com.simonalong.glue.connect;

import com.simonalong.glue.ChannelHelper;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
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

    private Map<String, ServerNettyConnector> connectorMap = new ConcurrentHashMap<>();

    @Override
    public void addConnect(Connector connector) {
        Channel channel = connector.getChannel();
        String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        connectorMap.computeIfAbsent(addr, address -> new ServerNettyConnector(channel, address));
    }

    public void addConnect(Channel channel){
        final String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if (connectorMap.containsKey(addr)) {
            return;
        }

        connectorMap.put(addr, new ServerNettyConnector(channel, addr));
    }

    @Override
    public void closeConnect(Channel channel) {
        String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if(!connectorMap.containsKey(addr)){
            return;
        }
        try {
            connectorMap.get(addr).close();
        } finally {
            connectorMap.remove(addr);
        }
    }

    public ServerNettyConnector getConnector(String addr) {
        if (connectorMap.containsKey(addr)) {
            return null;
        }

        return connectorMap.get(addr);
    }

    public Collection<ServerNettyConnector> getAllConnector(){
        return connectorMap.values();
    }
}
