package com.simonalong.glue.connect;

import com.simonalong.glue.GlueConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shizi
 * @since 2020/3/12 下午11:44
 */
@Slf4j
public abstract class AbstractConnector implements Connector {

    @Override
    public void close() {
        getChannel().close().addListener(future -> log.info(GlueConstant.LOG_PRE + "closeChannel: close the connection to remote address[{}] result: {}", getAddr(), future.isSuccess()));
    }
}
