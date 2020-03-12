package com.github.glue;

import com.github.glue.connect.ClientConnectManager;
import com.github.glue.connect.NettyClientConnector;
import com.github.glue.coder.NettyDecoder;
import com.github.glue.coder.NettyEncoder;
import com.github.glue.connect.NettyReceiverChannelManager;
import com.github.glue.event.CommandEventDispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.glue.GlueConstant.DEFAULT_GROUP_STR;

/**
 * @author shizi
 * @since 2020/3/3 下午12:18
 */
@Slf4j
public class NettyClient extends AbstractRemote {

    private static final NettyClient INSTANCE = new NettyClient();
    private volatile boolean startFlag = false;
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroupWorker;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    /**
     * 心跳时间：默认1分钟
     */
    @Setter
    private int heartTime = 60;
    @Setter
    private int workerThreadPoolSize = Runtime.getRuntime().availableProcessors();
    @Setter
    private int sndBufSize = 65535;
    @Setter
    private int rcvBufSize = 65535;
    @Setter
    private int connectTimeoutMillis = 3000;
    private ClientConnectManager clientConnectManager;
    /**
     * 链接的服务端的地址
     */
    private Set<String> connectAddrList;

    private NettyClient() {
        init();
    }

    public static NettyClient getInstance() {
        return INSTANCE;
    }

    private void init() {
        this.eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("Netty_Client_Selector_%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(workerThreadPoolSize, new ThreadFactory() {

            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Netty_Client_Worker_" + this.threadIndex.incrementAndGet());
            }
        });

        bootstrap = new Bootstrap();
        clientConnectManager = new ClientConnectManager(bootstrap);
        connectAddrList = new HashSet<>(12);
    }

    public synchronized void start() {
        if (startFlag) {
            return;
        }
        this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
            // 禁用小数据拼接后发送
            .option(ChannelOption.TCP_NODELAY, true)
            // 长连接标示，客户端和服务端要保持一致，否则有问题
            .option(ChannelOption.SO_KEEPALIVE, false)
            // 链接超时（毫秒，默认3秒）
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
            // 发送端缓冲区大小（单位Byte，默认 65535，即64k）
            .option(ChannelOption.SO_SNDBUF, sndBufSize)
            // 接收端缓冲区大小（单位Byte，默认 65535，即64k）
            .option(ChannelOption.SO_RCVBUF, rcvBufSize).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline()
                    .addLast(defaultEventExecutorGroup, new NettyEncoder())
                    .addLast(defaultEventExecutorGroup, new NettyDecoder())
                    .addLast(defaultEventExecutorGroup, new IdleStateHandler(0, 0, heartTime))
                    .addLast(defaultEventExecutorGroup, new NettyConnectHandler())
                    .addLast(defaultEventExecutorGroup, new NettyClientHandler());
            }
        });

        connectAddrList.forEach(this::initConnect);

        startFlag = true;
    }

    public void addConnect(String addr) {
        if (connectAddrList.add(addr)) {
            // 启动
            if (startFlag) {
                initConnect(addr);
            }
        }
    }

    public <T> NettySender<T> getSender(String addr, String group, String cmd, Class<T> tClass) {
        NettyClientConnector nettyClientConnector = clientConnectManager.getConnector(addr);
        if (null == nettyClientConnector) {
            return null;
        }
        return nettyClientConnector.asSender(group, cmd, tClass);
    }

    public <T> NettySender getSender(String addr, String cmd, Class<T> tClass) {
        return getSender(addr, DEFAULT_GROUP_STR, cmd, tClass);
    }

    private void initConnect(String addr) {
        try {
            clientConnectManager.addConnect(addr);
        } catch (InterruptedException e) {
            log.error("addConnect exception", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Boolean send(String addr, String group, String cmd, Object data) {
        return clientConnectManager.getConnector(addr).asSender(group, cmd).send(data);
    }

    public Boolean send(String addr, String cmd, Object data) {
        return send(addr, DEFAULT_GROUP_STR, cmd, data);
    }

    @SuppressWarnings("unchecked")
    public Boolean send(String addr, NettyCommand request) {
        return clientConnectManager.getConnector(addr).asSender(request.getEvent()).send(request);
    }

    @SuppressWarnings("unchecked")
    public void sendAsync(String addr, NettyCommand request, Runnable successCall, Runnable failCall) {
        clientConnectManager.getConnector(addr).asSender(request.getEvent()).sendAsync(request.getData(), successCall, failCall);
    }

    class NettyClientHandler extends SimpleChannelInboundHandler<NettyCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, NettyCommand request) {
            CommandEventDispatcher.dispatch(ctx, request);
        }
    }

    class NettyConnectHandler extends ChannelDuplexHandler {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("netty client pipeline: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("netty client pipeline: channelUnregistered, the channel[{}]", remoteAddress);
            super.channelUnregistered(ctx);
        }

        /**
         * 通道激活
         *
         * @param ctx 上下文
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("netty client pipeline: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);

            NettyReceiverChannelManager.getOrAddChannel(ctx.channel());
        }

        /**
         * 通道去激活
         *
         * @param ctx 上下文
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("netty client pipeline: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            NettyReceiverChannelManager.closeChannel(ctx.channel());
        }

        /**
         * 心跳事件
         *
         * @param ctx 上下文
         * @param evt 读写空闲事件
         * @throws Exception 异常
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("netty client pipeline: IDLE exception [{}]", remoteAddress);
                    NettyReceiverChannelManager.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("netty client pipeline: exceptionCaught {}", remoteAddress);
            log.warn("netty client pipeline: exceptionCaught exception.", cause);

            NettyReceiverChannelManager.closeChannel(ctx.channel());
        }
    }
}
