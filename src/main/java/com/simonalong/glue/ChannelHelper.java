package com.simonalong.glue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author shizi
 * @since 2020/3/3 下午3:02
 */
@Slf4j
@UtilityClass
public class ChannelHelper {

    /**
     * 解析对端地址
     *
     * @param channel channel
     * @return ip地址
     */
    public String parseChannelRemoteAddress(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String address = remote != null ? remote.toString() : "";

        if (address.length() > 0) {
            int index = address.lastIndexOf("/");
            if (index >= 0) {
                return address.substring(index + 1);
            }

            return address;
        }

        return "";
    }

    public void closeChannel(Channel channel) {
        final String addressRemote = parseChannelRemoteAddress(channel);
        channel.close()
            .addListener((ChannelFutureListener) future -> log.info(GlueConstant.LOG_PRE + "closeChannel: close the connection to remote address[{}] result: {}", addressRemote, future.isSuccess()));
    }

    public SocketAddress string2SocketAddress(final String address) {
        int split = address.lastIndexOf(":");
        String host = address.substring(0, split);
        String port = address.substring(split + 1);
        return new InetSocketAddress(host, Integer.parseInt(port));
    }
}
