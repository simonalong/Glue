package com.simonalong.glue;

import com.simonalong.glue.connect.ClientConnectManager;
import com.simonalong.glue.connect.ClientNettyConnector;
import com.simonalong.glue.coder.NettyDecoder;
import com.simonalong.glue.coder.NettyEncoder;
import com.simonalong.glue.event.CommandEventDispatcher;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.simonalong.glue.GlueConstant.DEFAULT_GROUP_STR;
import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/3 下午12:18
 */
@Slf4j
public class NettyClient extends AbstractRemote {

    private static final NettyClient INSTANCE = new NettyClient();
    private volatile boolean started = false;
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
    private ClientConnectManager connectManager;
    /**
     * 链接的服务端的地址
     */
    private Set<String> connectAddrList;
    @SuppressWarnings("rawtypes")
    private Map<ChannelOption, Object> channelOptionObjectMap;

    private NettyClient() {
        init();
    }

    public static NettyClient getInstance() {
        return INSTANCE;
    }

    private void init() {
        this.eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("Netty_Client_Selector_%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(workerThreadPoolSize, new ThreadFactory() {

            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Netty_Client_Worker_" + this.threadIndex.incrementAndGet());
            }
        });

        bootstrap = new Bootstrap();
        connectManager = new ClientConnectManager(bootstrap);
        connectAddrList = new HashSet<>(12);
        channelOptionObjectMap = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public synchronized void start() {
        if (started) {
            return;
        }
        // NioSocketChannel：异步的客户端 TCP Socket 连接.
        bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class);
        // 默认：禁用小数据拼接后发送
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        // 默认：不采用keepalive方式的长连接，采用心跳方式的长连接
        bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
        // 默认：链接超时（毫秒，默认3秒）
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
        // 默认：发送端缓冲区大小（单位Byte，默认 65535，即64k）
        bootstrap.option(ChannelOption.SO_SNDBUF, sndBufSize);
        // 默认：接收端缓冲区大小（单位Byte，默认 65535，即64k）
        bootstrap.option(ChannelOption.SO_RCVBUF, rcvBufSize);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
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
        channelOptionObjectMap.forEach((k, v) -> bootstrap.option(k, v));
        started = true;
    }

    public void addConnect(String addr) {
        if (connectAddrList.add(addr)) {
            if (started) {
                initConnect(addr);
            }
        }
    }

    public <T> void addOption(ChannelOption<T> option, T value) {
        channelOptionObjectMap.put(option, value);
    }

    public <T> NettySender<T> getSender(String address, String group, String cmd, Class<T> tClass) {
        ClientNettyConnector clientNettyConnector = connectManager.getConnector(address);
        if (null == clientNettyConnector) {
            return null;
        }
        return clientNettyConnector.asSender(group, cmd, tClass);
    }

    public <T> NettySender<T> getSender(String address, String cmd, Class<T> tClass) {
        return getSender(address, DEFAULT_GROUP_STR, cmd, tClass);
    }

    private void initConnect(String address) {
        connectManager.addConnect(address);
    }

    @SuppressWarnings("unchecked")
    public Boolean send(String address, String group, String cmd, Object data) {
        ClientNettyConnector connector = connectManager.getConnector(address);
        if (null != connector) {
            return connector.asSender(group, cmd).send(data);
        }
        log.warn(LOG_PRE + "the connector of addr[{}] not available", address);
        return false;
    }

    public Boolean send(String addr, String cmd, Object data) {
        return send(addr, DEFAULT_GROUP_STR, cmd, data);
    }

    @SuppressWarnings("unchecked")
    public Boolean send(String address, NettyCommand request) {
        ClientNettyConnector connector = connectManager.getConnector(address);
        if (null != connector) {
            return connector.asSender(request.getEvent()).send(request.getData());
        }
        log.warn(LOG_PRE + "the connector of addr[{}] not available", address);
        return false;
    }

    @SuppressWarnings("unchecked")
    public void sendAsync(String address, NettyCommand request, Runnable successCall, Runnable failCall) {
        ClientNettyConnector connector = connectManager.getConnector(address);
        if (null != connector) {
            connector.asSender(request.getEvent()).sendAsync(request.getData(), successCall, failCall);
        }
        log.warn(LOG_PRE + "the connector of addr[{}] not available", address);
    }

    static class NettyClientHandler extends SimpleChannelInboundHandler<NettyCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, NettyCommand request) {
            CommandEventDispatcher.dispatch(ctx, request);
        }
    }

    class NettyConnectHandler extends ChannelDuplexHandler {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty client pipeline: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty client pipeline: channelUnregistered, the channel[{}]", remoteAddress);
            super.channelUnregistered(ctx);
        }

        /**
         * 通道激活
         *
         * @param ctx 上下文
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty client pipeline: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);
        }

        /**
         * 通道去激活
         *
         * @param ctx 上下文
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty client pipeline: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            connectManager.closeConnect(ctx.channel());
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
                    final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
                    log.warn(LOG_PRE + "netty client pipeline: IDLE exception [{}]", remoteAddress);
                    connectManager.closeConnect(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.warn(LOG_PRE + "netty client pipeline: address [{}],  exceptionCaught exception.", remoteAddress, cause);

            connectManager.closeConnect(ctx.channel());
        }
    }
}
