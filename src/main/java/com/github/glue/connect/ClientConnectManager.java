package com.github.glue.connect;

import com.github.glue.ChannelHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.glue.GlueConstant.LOG_PRE;

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
     * @param addr 链接地址
     * @return true：处理后地址可用，false：地址不存在或者不可用
     */
    public synchronized Boolean addConnect(String addr) {
        if (connectorMap.containsKey(addr)) {
            ClientNettyConnector clientConnector = connectorMap.get(addr);
            if (clientConnector.isOK()) {
                return true;
            } else if (!clientConnector.getChannelFuture().isDone()) {
                return true;
            } else {
                connectorMap.remove(addr);
            }
        }

        ChannelFuture channelFuture = bootstrap.connect(ChannelHelper.string2SocketAddress(addr));
        if (channelFuture.awaitUninterruptibly(connectTimeoutMillis)) {
            if (null != channelFuture.channel() && channelFuture.channel().isActive()) {
                log.info(LOG_PRE + "createChannel: addConnect remote host[{}] success, {}", addr, channelFuture.toString());
                connectorMap.put(addr, new ClientNettyConnector(channelFuture, addr));
                return true;
            } else {
                log.warn(LOG_PRE + "createChannel: addConnect remote host[" + addr + "] failed, " + channelFuture.toString(), channelFuture.cause());
            }
        } else {
            log.warn(LOG_PRE + "createChannel: addConnect remote host[{}] timeout {}ms, {}", addr, connectTimeoutMillis, channelFuture.toString());
        }
        return false;
    }

    @Override
    public synchronized void addConnect(Connector connector) {
        if (!(connector instanceof ClientNettyConnector)) {
            return;
        }
        String addr = connector.getAddr();
        if (connectorMap.containsKey(addr)) {
            try {
                connectorMap.get(addr).close();
            } finally {
                connectorMap.remove(addr);
            }
        }

        connectorMap.put(addr, (ClientNettyConnector) connector);
        log.info(LOG_PRE + "createChannel: begin to addConnect remote host[{}] asynchronously", addr);
    }

    @Override
    public void closeConnect(Channel channel) {
        String addr = ChannelHelper.parseChannelRemoteAddr(channel);
        if (!connectorMap.containsKey(addr)) {
            return;
        }
        try {
            connectorMap.get(addr).close();
        } finally {
            connectorMap.remove(addr);
        }
    }

    public ClientNettyConnector getConnector(String addr) {
        if (!connectorMap.containsKey(addr)) {
            if (!addConnect(addr)) {
                return null;
            }
        }
        return connectorMap.get(addr);
    }
}
