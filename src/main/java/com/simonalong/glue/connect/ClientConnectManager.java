package com.simonalong.glue.connect;

import com.simonalong.glue.ChannelHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * 客户端的链接管理器
 *
 * @author shizi
 * @since 2020/3/4 下午5:46
 */
@Slf4j
public class ClientConnectManager implements ConnectManager {

    private Bootstrap bootstrap;
    private Map<String, ClientNettyConnector> connectorMap = new ConcurrentHashMap<>();
    @Setter
    private int connectTimeoutMillis = 3000;

    public ClientConnectManager(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * 添加链接
     *
     * @param address 链接地址
     * @return true：处理后地址可用，false：地址不存在或者不可用
     */
    public synchronized Boolean addConnect(String address) {
        if (connectorMap.containsKey(address)) {
            ClientNettyConnector clientConnector = connectorMap.get(address);
            if (clientConnector.isOK()) {
                return true;
            } else if (!clientConnector.getChannelFuture().isDone()) {
                return true;
            } else {
                connectorMap.remove(address);
            }
        }

        ChannelFuture channelFuture = bootstrap.connect(ChannelHelper.string2SocketAddress(address));
        if (channelFuture.awaitUninterruptibly(connectTimeoutMillis)) {
            if (null != channelFuture.channel() && channelFuture.channel().isActive()) {
                log.info(LOG_PRE + "createChannel: addConnect remote host[{}] success, {}", address, channelFuture.toString());
                connectorMap.put(address, new ClientNettyConnector(channelFuture, address));
                return true;
            } else {
                log.warn(LOG_PRE + "createChannel: addConnect remote host[" + address + "] failed, " + channelFuture.toString(), channelFuture.cause());
            }
        } else {
            log.warn(LOG_PRE + "createChannel: addConnect remote host[{}] timeout {}ms, {}", address, connectTimeoutMillis, channelFuture.toString());
        }
        return false;
    }

    @Override
    public synchronized void addConnect(Connector connector) {
        if (!(connector instanceof ClientNettyConnector)) {
            return;
        }
        String address = connector.getAddress();
        if (connectorMap.containsKey(address)) {
            try {
                connectorMap.get(address).close();
            } finally {
                connectorMap.remove(address);
            }
        }

        connectorMap.put(address, (ClientNettyConnector) connector);
        log.info(LOG_PRE + "createChannel: begin to addConnect remote host[{}] asynchronously", address);
    }

    @Override
    public void closeConnect(Channel channel) {
        String address = ChannelHelper.parseChannelRemoteAddress(channel);
        if (!connectorMap.containsKey(address)) {
            return;
        }
        try {
            connectorMap.get(address).close();
        } finally {
            connectorMap.remove(address);
        }
    }

    public ClientNettyConnector getConnector(String address) {
        if (!connectorMap.containsKey(address)) {
            if (!addConnect(address)) {
                return null;
            }
        }
        return connectorMap.get(address);
    }
}
