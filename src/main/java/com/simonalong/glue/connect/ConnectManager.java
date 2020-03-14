package com.simonalong.glue.connect;

import io.netty.channel.Channel;

/**
 * @author shizi
 * @since 2020/3/12 下午10:22
 */
public interface ConnectManager {

    /**
     * 添加链接
     * @param connector 连接器
     */
    void addConnect(Connector connector);

    /**
     * 关闭连接器
     *
     * @param channel 关闭链接对应的通道
     */
    void closeConnect(Channel channel);
}
