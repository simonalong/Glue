package com.github.glue.connect;

import com.github.glue.ChannelHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shizi
 * @since 2020/3/3 下午4:18
 */
@Slf4j
@UtilityClass
public class NettyReceiverChannelManager {

    private static Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public Channel getOrAddChannel(Channel channel) {
        String channelAddress = ChannelHelper.parseChannelRemoteAddr(channel);
        return channelMap.putIfAbsent(channelAddress, channel);
    }

    public void closeChannel(Channel channel) {
        final String addrRemote = ChannelHelper.parseChannelRemoteAddr(channel);
        try {
            channel.close()
                .addListener((ChannelFutureListener) future -> log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote, future.isSuccess()));
        } finally {
            channelMap.remove(addrRemote);
        }
    }
}
