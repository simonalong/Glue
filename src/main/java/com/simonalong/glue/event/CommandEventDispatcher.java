package com.simonalong.glue.event;

import com.simonalong.glue.NettyCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/3 下午6:20
 */
@Slf4j
@UtilityClass
public final class CommandEventDispatcher {

    private final Map<String, CommandProcessor> channelPairMap = new ConcurrentHashMap<>();

    public void addCmdProcessor(CommandProcessor commandProcessor) {
        if (null != commandProcessor) {
            channelPairMap.putIfAbsent(commandProcessor.getGroup(), commandProcessor);
        }
    }

    public void dispatch(ChannelHandlerContext ctx, NettyCommand request) {
        if (channelPairMap.containsKey(request.getEvent().getGroup())) {
            channelPairMap.get(request.getEvent().getGroup()).process(ctx, request);
        } else {
            log.warn(LOG_PRE + "discard msg: because not found the processor of command:[{}]", request.getEvent());
        }
    }
}
