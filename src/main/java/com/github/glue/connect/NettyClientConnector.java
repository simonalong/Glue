package com.github.glue.connect;

import com.github.glue.NettySender;
import com.github.glue.event.CommandEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * @author shizi
 * @since 2020/3/4 下午7:03
 */
public class NettyClientConnector {

    private final ChannelFuture channelFuture;
    private String addr;

    public NettyClientConnector(ChannelFuture channelFuture, String addr) {
        this.channelFuture = channelFuture;
        this.addr = addr;
    }

    public boolean isOK() {
        return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
    }

    public boolean isWritable() {
        return this.channelFuture.channel().isWritable();
    }

    public Channel getChannel() {
        return this.channelFuture.channel();
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public <T> NettySender<T> asSender(String group, String cmd, Class<T> tClass) {
        NettySender<T> sender = new NettySender<>();
        sender.setChannel(getChannel());
        sender.setAddr(addr);
        sender.setCmd(cmd);
        sender.setGroup(group);
        sender.setTClass(tClass);
        return sender;
    }

    public NettySender asSender(String group, String cmd) {
        NettySender sender = new NettySender();
        sender.setChannel(getChannel());
        sender.setAddr(addr);
        sender.setCmd(cmd);
        sender.setGroup(group);
        return sender;
    }

    public <T> NettySender<T> asSender(CommandEvent event, Class<T> tClass) {
        return asSender(event.getGroup(), event.getCmd(), tClass);
    }

    public NettySender asSender(CommandEvent event) {
        return asSender(event.getGroup(), event.getCmd());
    }
}
