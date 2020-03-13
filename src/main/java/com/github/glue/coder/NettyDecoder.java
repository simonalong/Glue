package com.github.glue.coder;

import com.github.glue.NettyCommand;
import com.github.glue.ChannelHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.github.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/4 下午2:09
 */
@Slf4j
public class NettyDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            NettyCommand command = NettyCommand.decode(msg);
            if(null != command) {
                out.add(command);
            }
        } catch (Exception e) {
            log.error(LOG_PRE + "decode exception, " + ChannelHelper.parseChannelRemoteAddr(ctx.channel()), e);
            ChannelHelper.closeChannel(ctx.channel());
        }
    }
}
