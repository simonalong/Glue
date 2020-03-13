package com.github.glue.coder;

import com.alibaba.fastjson.JSON;
import com.github.glue.NettyCommand;
import com.github.glue.ChannelHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

import static com.github.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/3 下午1:28
 */
@Slf4j
@ChannelHandler.Sharable
public class NettyEncoder extends MessageToByteEncoder<NettyCommand> {

    private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    @Override
    public void encode(ChannelHandlerContext ctx, NettyCommand request, ByteBuf out) {
        try {
            byte[] bodyByte = JSON.toJSONString(request).getBytes(CHARSET_UTF8);
            out.writeBytes(request.encodeHead(bodyByte.length));
            out.writeBytes(bodyByte);
        } catch (Throwable e) {
            log.error(LOG_PRE + "encodeHead exception, " + ChannelHelper.parseChannelRemoteAddr(ctx.channel()), e);
            if (request != null) {
                log.error(request.toString());
            }
            ChannelHelper.closeChannel(ctx.channel());
        }
    }
}
