package com.github.glue.connect;

import com.github.glue.ChannelHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端的链接管理器
 *
 * @author shizi
 * @since 2020/3/4 下午5:46
 */
@Slf4j
public class ClientConnectManager implements ConnectManager {

    private Bootstrap bootstrap;
    private Map<String, NettyClientConnector> connectorMap = new ConcurrentHashMap<>();
    @Setter
    private int connectTimeoutMillis = 3000;

    public ClientConnectManager(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public synchronized void addConnect(String addr) {
        if (connectorMap.containsKey(addr)) {
            NettyClientConnector clientConnector = connectorMap.get(addr);
            if (clientConnector.getChannelFuture().isDone()) {
                return;
            } else {
                connectorMap.remove(addr);
            }
        }

        ChannelFuture channelFuture = this.bootstrap.connect(ChannelHelper.string2SocketAddress(addr));
        this.connectorMap.put(addr, new NettyClientConnector(channelFuture, addr));
        log.info("createChannel: begin to addConnect remote host[{}] asynchronously", addr);
    }

    @Override
    public synchronized void addConnect(Connector connector) {
        if (!(connector instanceof NettyClientConnector)) {
            return;
        }
        String addr = connector.getAddr();
        if (connectorMap.containsKey(addr)) {
            try {
                connectorMap.get(addr).close();
            } catch (Exception ignored) {

            }
            connectorMap.remove(addr);
        }

        connectorMap.put(addr, (NettyClientConnector) connector);
        log.info("createChannel: begin to addConnect remote host[{}] asynchronously", addr);
    }

    @Override
    public void closeConnect(Channel channel) {
        channel.close()
            .addListener(
                future -> log.info("closeChannel: close the connection to remote address[{}] result: {}", ChannelHelper.parseChannelRemoteAddr(channel), future.isSuccess()));
    }

    public NettyClientConnector getConnector(String addr) {
        if (!connectorMap.containsKey(addr)) {
            return null;
        }

        NettyClientConnector connector = connectorMap.get(addr);

        if (connector != null) {
            ChannelFuture channelFuture = connector.getChannelFuture();
            if (channelFuture.awaitUninterruptibly(connectTimeoutMillis)) {
                if (connector.isOK()) {
                    log.info("createChannel: addConnect remote host[{}] success, {}", addr, channelFuture.toString());
                    return connector;
                } else {
                    log.warn("createChannel: addConnect remote host[" + addr + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                log.warn("createChannel: addConnect remote host[{}] timeout {}ms, {}", addr, connectTimeoutMillis, channelFuture.toString());
            }
        }
        return null;
    }
}
