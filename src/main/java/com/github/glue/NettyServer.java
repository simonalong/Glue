package com.github.glue;

import com.github.glue.coder.NettyDecoder;
import com.github.glue.coder.NettyEncoder;
import com.github.glue.connect.NettyReceiverChannelManager;
import com.github.glue.event.CommandEventDispatcher;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shizi
 * @since 2020/3/3 下午12:18
 */
@Slf4j
public class NettyServer extends AbstractRemote {

    private static final NettyServer INSTANCE = new NettyServer();
    private Boolean initFlag = false;
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
        this.initFlag = true;
        return this;
    }

    public void start() {
        if (!initFlag) {
            throw new RuntimeException("please first init");
        }

        this.serverBootstrap.group(this.boss, this.worker).channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            // 链接队列大小
            .option(ChannelOption.SO_BACKLOG, 1024)
            // 允许重复使用本地地址和端口
            .option(ChannelOption.SO_REUSEADDR, true)
            // 禁止使用Nagle算法，使用于小数据即时传输
            .childOption(ChannelOption.TCP_NODELAY, true)
            // 内存配置
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            // 发送端缓冲区大小（单位Byte，默认 65535，即64k）
            .childOption(ChannelOption.SO_SNDBUF, sndBufSize)
            // 接收端缓冲区大小（单位Byte，默认 65535，即64k）
            .childOption(ChannelOption.SO_RCVBUF, rcvBufSize).childHandler(new ChannelInitializer<SocketChannel>() {
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

        try {
            ChannelFuture channelFuture = serverBootstrap.bind(ChannelHelper.string2SocketAddress(addr)).sync();
            if (channelFuture.isSuccess()) {
                log.info("netty server start success");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e);
        }
    }

    public <T> void addOption(ChannelOption<T> option, T value) {
        channelOptionObjectMap.put(option, value);
    }

    public <T> void addChildOption(ChannelOption<T> option, T value) {
        channelChildOptionObjectMap.put(option, value);
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
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("netty server pipeline: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("netty server pipeline: channelUnregistered, the channel[{}]", remoteAddress);
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
            log.info("netty server pipeline: channelActive, the channel[{}]", remoteAddress);
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
            log.info("netty server pipeline: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            NettyReceiverChannelManager.closeChannel(ctx.channel());
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
                    final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("netty server pipeline: IDLE exception [{}]", remoteAddress);
                    NettyReceiverChannelManager.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            final String remoteAddress = ChannelHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("netty server pipeline: exceptionCaught {}", remoteAddress);
            log.warn("netty server pipeline: exceptionCaught exception.", cause);

            NettyReceiverChannelManager.closeChannel(ctx.channel());
        }
    }
}
