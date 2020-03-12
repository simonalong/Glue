package com.github.glue.connect;

import com.github.glue.NettySender;
import com.github.glue.event.CommandEvent;

import static com.github.glue.GlueConstant.DEFAULT_GROUP_STR;

/**
 * @author shizi
 * @since 2020/3/12 下午9:43
 */
public abstract class AbstractConnectSender extends AbstractConnector implements Connector {

    public <T> NettySender<T> asSender(String group, String cmd, Class<T> tClass) {
        NettySender<T> sender = new NettySender<>();
        sender.setChannel(getChannel());
        sender.setAddr(getAddr());
        sender.setCmd(cmd);
        sender.setGroup(group);
        sender.setTClass(tClass);
        return sender;
    }

    public NettySender asSender(String group, String cmd) {
        return asSender(group, cmd, Object.class);
    }

    public <T> NettySender<T> asSender(String cmd, Class<T> tClass) {
        return asSender(DEFAULT_GROUP_STR, cmd, tClass);
    }

    public NettySender asSender(String cmd) {
        return asSender(DEFAULT_GROUP_STR, cmd, Object.class);
    }

    public <T> NettySender<T> asSender(CommandEvent event, Class<T> tClass) {
        return asSender(event.getGroup(), event.getCmd(), tClass);
    }

    public NettySender asSender(CommandEvent event) {
        return asSender(event.getGroup(), event.getCmd(), Object.class);
    }
}
