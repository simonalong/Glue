package com.simonalong.glue;

import lombok.Data;

import java.io.Serializable;

/**
 * @author shizi
 * @since 2020/3/4 下午8:36
 */
@Data
public class NettyErrorResponse implements Serializable {

    /**
     * 请求的实体
     */
    private NettyCommand command;
    /**
     * 服务端的异常信息
     */
    private String errMsg;
}
