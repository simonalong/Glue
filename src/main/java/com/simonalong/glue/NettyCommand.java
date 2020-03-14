package com.simonalong.glue;

import com.alibaba.fastjson.JSON;
import com.simonalong.glue.event.CommandEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.Arrays;

import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/3 下午1:33
 */
@Slf4j
@ToString(of = {"event", "data"})
@NoArgsConstructor
public class NettyCommand {

    private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    /**
     * 用字符 glue（胶水） 作为消息标识
     */
    private final static byte[] TAG = "glue".getBytes(CHARSET_UTF8);
    @Getter
    @Setter
    private CommandEvent event;
    @Setter
    @Getter
    private Object data;

    public NettyCommand(String group, String cmd, Object data) {
        this.event = new CommandEvent(group, cmd);
        this.data = data;
    }

    public NettyCommand(String cmd, Object data) {
        this.event = new CommandEvent(cmd);
        this.data = data;
    }

    public NettyCommand(String group, String cmd) {
        this.event = new CommandEvent(group, cmd);
    }

    public NettyCommand(String cmd) {
        this.event = new CommandEvent(cmd);
    }

    /**
     * 采用tlv 方式编码：tag + length + value
     *
     * @return ByteBuffer数据
     */
    public ByteBuf encodeHead(int bodyLength) {
        ByteBuf headBuf = Unpooled.buffer(8);
        headBuf.writeBytes(TAG);
        // 1> tag value
        int length = 4;
        // 2> length value
        length += 4;
        // 3> data data length
        length += bodyLength;
        headBuf.writeInt(length);
        return headBuf;
    }

    public static NettyCommand decode(ByteBuf byteBuffer) {
        byte[] tagBytes = new byte[4];
        byteBuffer.readBytes(tagBytes);
        if (!Arrays.equals(tagBytes, TAG)) {
            String notSupportMsgTag = new String(tagBytes, CHARSET_UTF8);
            log.warn(LOG_PRE + "not support message tag: {}", notSupportMsgTag);
            return null;
        }

        // 读取body数据
        int length = byteBuffer.readInt();
        if (length > 0) {
            // 减去TAG的长度，和存储数据length的长度
            byte[] bodyBytes = new byte[length - 4 - 4];
            byteBuffer.readBytes(bodyBytes);

            String tem = new String(bodyBytes, CHARSET_UTF8);
            return JSON.parseObject(tem, NettyCommand.class);
        }
        return null;
    }
}
