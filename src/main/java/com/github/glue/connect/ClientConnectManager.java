package com.github.glue.connect;

import com.github.glue.ChannelHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.glue.GlueConstant.LOCK_TIMEOUT_MILLIS;

/**
 * @author shizi
 * @since 2020/3/4 下午5:46
 */
@Slf4j
public class ClientConnectManager {

    private Bootstrap bootstrap;
    private Map<String, NettyClientConnector> channelMap = new ConcurrentHashMap<>();
    private final Lock lockChannelTables = new ReentrantLock();
    @Setter
    private int connectTimeoutMillis = 3000;

    public ClientConnectManager(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public NettyClientConnector getConnector(String addr) {
        NettyClientConnector cw = this.channelMap.get(addr);
        if (cw != null && cw.isOK()) {
            return cw;
        }

        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean createNewConnection;
                    cw = this.channelMap.get(addr);
                    if (cw != null) {
                        if (cw.isOK()) {
                            return cw;
                        } else if (!cw.getChannelFuture().isDone()) {
                            createNewConnection = false;
                        } else {
                            this.channelMap.remove(addr);
                            createNewConnection = true;
                        }
                    } else {
                        createNewConnection = true;
                    }

                    if (createNewConnection) {
                        ChannelFuture channelFuture = this.bootstrap.connect(ChannelHelper.string2SocketAddress(addr));
                        log.info("createChannel: begin to addConnect remote host[{}] asynchronously", addr);
                        cw = new NettyClientConnector(channelFuture, addr);
                        this.channelMap.put(addr, cw);
                    }
                } catch (Exception e) {
                    log.error("createChannel: create channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (cw != null) {
            ChannelFuture channelFuture = cw.getChannelFuture();
            if (channelFuture.awaitUninterruptibly(connectTimeoutMillis)) {
                if (cw.isOK()) {
                    log.info("createChannel: addConnect remote host[{}] success, {}", addr, channelFuture.toString());
                    return cw;
                } else {
                    log.warn("createChannel: addConnect remote host[" + addr + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                log.warn("createChannel: addConnect remote host[{}] timeout {}ms, {}", addr, connectTimeoutMillis, channelFuture.toString());
            }
        }
        return null;
    }

    public Channel getChannel(String addr) throws InterruptedException {
        return getConnector(addr).getChannel();
    }

    public void addConnect(String addr) throws InterruptedException {
        getChannel(addr);
    }
}
