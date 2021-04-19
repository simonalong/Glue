package com.simonalong.glue.connect;

import com.simonalong.glue.NettySender;
import com.simonalong.glue.event.CommandEvent;
import com.simonalong.glue.GlueConstant;

/**
 * @author shizi
 * @since 2020/3/12 下午9:43
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractConnectSender extends AbstractConnector implements Connector {

    public <T> NettySender<T> asSender(String group, String cmd, Class<T> tClass) {
        NettySender<T> sender = new NettySender<>();
        sender.setChannel(getChannel());
        sender.setAddress(getAddress());
        sender.setCmd(cmd);
        sender.setGroup(group);
        sender.setTClass(tClass);
        return sender;
    }

    public NettySender asSender(String group, String cmd) {
        return asSender(group, cmd, Object.class);
    }

    public <T> NettySender<T> asSender(String cmd, Class<T> tClass) {
        return asSender(GlueConstant.DEFAULT_GROUP_STR, cmd, tClass);
    }

    public NettySender asSender(String cmd) {
        return asSender(GlueConstant.DEFAULT_GROUP_STR, cmd, Object.class);
    }

    public <T> NettySender<T> asSender(CommandEvent event, Class<T> tClass) {
        return asSender(event.getGroup(), event.getCmd(), tClass);
    }

    public NettySender asSender(CommandEvent event) {
        return asSender(event.getGroup(), event.getCmd(), Object.class);
    }
}
