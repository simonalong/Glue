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

    private final Map<String, ServerNettyConnector> connectorMap = new ConcurrentHashMap<>();

    @Override
    public void addConnect(Connector connector) {
        Channel channel = connector.getChannel();
        String address = ChannelHelper.parseChannelRemoteAddress(channel);
        connectorMap.computeIfAbsent(address, addressTem -> new ServerNettyConnector(channel, addressTem));
    }

    public void addConnect(Channel channel){
        final String address = ChannelHelper.parseChannelRemoteAddress(channel);
        if (connectorMap.containsKey(address)) {
            return;
        }

        connectorMap.put(address, new ServerNettyConnector(channel, address));
    }

    @Override
    public void closeConnect(Channel channel) {
        String address = ChannelHelper.parseChannelRemoteAddress(channel);
        if(!connectorMap.containsKey(address)){
            return;
        }
        try {
            connectorMap.get(address).close();
        } finally {
            connectorMap.remove(address);
        }
    }

    public ServerNettyConnector getConnector(String address) {
        if (connectorMap.containsKey(address)) {
            return null;
        }

        return connectorMap.get(address);
    }

    public Collection<ServerNettyConnector> getAllConnector(){
        return connectorMap.values();
    }
}
