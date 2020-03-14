package com.simonalong.glue.connect;

import io.netty.channel.Channel;

/**
 * @author shizi
 * @since 2020/3/12 下午9:41
 */
public interface Connector extends AutoCloseable{

    /**
     * 获取Channel
     * @return channel
     */
    Channel getChannel();

    /**
     * 连接器对应的地址
     * @return 地址
     */
    String getAddr();

    /**
     * 通道是否可用
     * @return true：可用，false：不可用
     */
    boolean isOK();
}
