package com.simonalong.glue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/4 下午6:03
 */
@Slf4j
@NoArgsConstructor
@Getter
@Setter
public class NettySender<T> {

    private String group;
    private String cmd;
    private Channel channel;
    private String addr;
    private Class<T> tClass;

    public Boolean send(T data) {
        NettyCommand command = new NettyCommand(group, cmd, data);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        channel.writeAndFlush(command).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                result.set(true);
                countDownLatch.countDown();
                return;
            }
            log.warn(LOG_PRE + "send command to channel [{}] failed.", addr);
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.warn(LOG_PRE + "thread interrupt", e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public void sendAsync(T data, Runnable successCall, Runnable failCall) {
        NettyCommand command = new NettyCommand(group, cmd, data);
        channel.writeAndFlush(command).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                log.info(LOG_PRE + "send command success");
                if (null != successCall) {
                    successCall.run();
                }
                return;
            }
            log.warn(LOG_PRE + "send command to channel [{}] failed.", addr);
            if (null != failCall) {
                failCall.run();
            }
        });
    }
}
