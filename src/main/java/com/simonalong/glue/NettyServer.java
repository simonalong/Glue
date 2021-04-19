package com.simonalong.glue;

import com.simonalong.glue.coder.NettyDecoder;
import com.simonalong.glue.coder.NettyEncoder;
import com.simonalong.glue.connect.ServerNettyConnector;
import com.simonalong.glue.connect.ServerConnectManager;
import com.simonalong.glue.event.CommandEventDispatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static com.simonalong.glue.GlueConstant.DEFAULT_GROUP_STR;
import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/3 下午12:18
 */
@Slf4j
public class NettyServer extends AbstractRemote {

    private static final NettyServer INSTANCE = new NettyServer();
    private volatile boolean started = false;
    private String addr;
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private ServerBootstrap serverBootstrap;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    /**
     * 心跳时间：默认1分钟
     */
    @Setter
    private int heartTime = 60;
    @Setter
    private int bossThreadPoolSize = 1;
    @Setter
    private int workerThreadPoolSize = Runtime.getRuntime().availableProcessors();
    @Setter
    private int sndBufSize = 65535;
    @Setter
    private int rcvBufSize = 65535;
    /**
     * make make install
     * <p>
     * <p>
     * ../glibc-2.10.1/configure \ --prefix=/usr \ --with-headers=/usr/include \
     * --host=x86_64-linux-gnu \ --build=x86_64-pc-linux-gnu \ --without-gd
     */
    @Setter
    private boolean useEpollNativeSelector = false;
    private static final String OS_NAME = System.getProperty("os.name");
    private NettyConnectHandler connectionHandler;
    private NettyServerHandler serverHandler;
    private NettyEncoder encoder;
    private Map<ChannelOption, Object> channelOptionObjectMap;
    private Map<ChannelOption, Object> channelChildOptionObjectMap;
    private ServerConnectManager connectManager;

    private NettyServer() {}

    public static NettyServer getInstance() {
        return INSTANCE;
    }

    public NettyServer bind(String addr) {
        this.addr = addr;
        if (useEpoll()) {
            this.boss = new EpollEventLoopGroup(bossThreadPoolSize, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("Netty_Epoll_Boss_%d", this.threadIndex.incrementAndGet()));
                }
            });

            this.worker = new EpollEventLoopGroup(workerThreadPoolSize, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("Netty_Epoll_Worker_%d_%d", workerThreadPoolSize, this.threadIndex.incrementAndGet()));
                }
            });
        } else {
            this.boss = new NioEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("Netty_Nio_Boss_%d", this.threadIndex.incrementAndGet()));
                }
            });

            this.worker = new NioEventLoopGroup(workerThreadPoolSize, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("Netty_Nio_Worker_%d_%d", workerThreadPoolSize, this.threadIndex.incrementAndGet()));
                }
            });
        }

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(workerThreadPoolSize, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Netty_Server_Codec_Thread_" + this.threadIndex.incrementAndGet());
            }
        });

        serverBootstrap = new ServerBootstrap();
        connectionHandler = new NettyConnectHandler();
        serverHandler = new NettyServerHandler();
        encoder = new NettyEncoder();
        channelOptionObjectMap = new HashMap<>(8);
        channelChildOptionObjectMap = new HashMap<>(8);
        connectManager = new ServerConnectManager();
        return this;
    }

    @SuppressWarnings("unchecked")
    public synchronized void start() {
        if (started) {
            return;
        }

        serverBootstrap.group(this.boss, this.worker);
        // NioServerSocketChannel：异步的服务器端 TCP Socket 连接.
        serverBootstrap.channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
        // 默认：链接队列大小
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        // 默认：允许重复使用本地地址和端口
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        // 默认：禁止使用Nagle算法，使用于小数据即时传输
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        // 默认：内存配置
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        // 默认：发送端缓冲区大小（单位Byte，默认 65535，即64k）
        serverBootstrap.childOption(ChannelOption.SO_SNDBUF, sndBufSize);
        // 默认：接收端缓冲区大小（单位Byte，默认 65535，即64k）
        serverBootstrap.childOption(ChannelOption.SO_RCVBUF, rcvBufSize);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline()
                    .addLast(defaultEventExecutorGroup, encoder)
                    .addLast(defaultEventExecutorGroup, new NettyDecoder())
                    .addLast(defaultEventExecutorGroup, new IdleStateHandler(0, 0, heartTime))
                    .addLast(defaultEventExecutorGroup, connectionHandler)
                    .addLast(defaultEventExecutorGroup, serverHandler);
            }
        });

        channelOptionObjectMap.forEach((k, v) -> serverBootstrap.option(k, v));
        channelChildOptionObjectMap.forEach((k, v) -> serverBootstrap.childOption(k, v));

        try {
            ChannelFuture channelFuture = serverBootstrap.bind(ChannelHelper.string2SocketAddress(addr)).sync();
            if (channelFuture.isSuccess()) {
                log.info(LOG_PRE + "netty server start success");
            }
            started = true;
        } catch (InterruptedException e) {
            log.warn(LOG_PRE + "this.serverBootstrap.bind().sync() InterruptedException", e);
            Thread.currentThread().interrupt();
        }
    }

    public <T> void addOption(ChannelOption<T> option, T value) {
        channelOptionObjectMap.put(option, value);
    }

    public <T> void addChildOption(ChannelOption<T> option, T value) {
        channelChildOptionObjectMap.put(option, value);
    }

    @SuppressWarnings("unchecked")
    public void sendAll(String group, String cmd, Object data) {
        Collection<ServerNettyConnector> serverConnectors = connectManager.getAllConnector();
        serverConnectors.forEach(connector -> connector.asSender(group, cmd).send(data));
    }

    @SuppressWarnings("unchecked")
    public void sendAll(String cmd, Object data) {
        Collection<ServerNettyConnector> serverConnectors = connectManager.getAllConnector();
        serverConnectors.forEach(connector -> connector.asSender(cmd).send(data));
    }

    public Boolean send(String addr, String group, String cmd, Object data) {
        return send(addr, new NettyCommand(group, cmd, data));
    }

    public Boolean send(String addr, String cmd, Object data) {
        return send(addr, DEFAULT_GROUP_STR, cmd, data);
    }

    @SuppressWarnings("unchecked")
    public Boolean send(String addr, NettyCommand nettyCommand) {
        ServerNettyConnector serverConnector = connectManager.getConnector(addr);
        if(null != serverConnector){
            serverConnector.asSender(nettyCommand.getEvent()).send(nettyCommand.getData());
            return true;
        }

        log.warn(LOG_PRE + "the connector of addr[{}] not available", addr);
        return false;
    }

    private boolean useEpoll() {
        return isLinuxPlatform() && useEpollNativeSelector && Epoll.isAvailable();
    }

    private boolean isLinuxPlatform() {
        return OS_NAME != null && OS_NAME.toLowerCase().contains("linux");
    }

    @ChannelHandler.Sharable
    class NettyServerHandler extends SimpleChannelInboundHandler<NettyCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, NettyCommand request) {
            CommandEventDispatcher.dispatch(ctx, request);
        }
    }

    @ChannelHandler.Sharable
    class NettyConnectHandler extends ChannelDuplexHandler {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty server pipeline: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty server pipeline: channelUnregistered, the channel[{}]", remoteAddress);
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
            log.info(LOG_PRE + "netty server pipeline: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);

            connectManager.addConnect(ctx.channel());
        }

        /**
         * 通道去激活
         *
         * @param ctx 上下文
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.info(LOG_PRE + "netty server pipeline: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            connectManager.closeConnect(ctx.channel());
        }

        /**
         * 心跳事件
         *
         * @param ctx 上下文
         * @param evt 读写空闲事件
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
                    log.warn(LOG_PRE + "netty server pipeline: IDLE exception [{}]", remoteAddress);

                    connectManager.closeConnect(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddress(ctx.channel());
            log.warn(LOG_PRE + "netty server pipeline: address [{}],  exceptionCaught exception.", remoteAddress, cause);

            connectManager.closeConnect(ctx.channel());
        }
    }
}
